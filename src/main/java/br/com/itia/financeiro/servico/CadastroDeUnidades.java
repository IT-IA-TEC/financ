package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.HistoricoDaUnidade;
import br.com.itia.financeiro.dominio.Pagador;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.HistoricoDaUnidadeRepositorio;
import br.com.itia.financeiro.repositorio.PagadorRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O cadastro da unidade: os dados comerciais e a memória das trocas.
 *
 * As regras que este serviço protege:
 *   1. Percentual, plataforma e tom de cobrança são da unidade, e não da
 *      pessoa. A mesma pessoa pode ter uma loja com um acordo e outra com
 *      outro.
 *   2. Troca de dono, de telefone e de percentual vira linha de histórico. A
 *      unidade é a mesma; o que mudou fica escrito, com data e autor.
 *   3. Na importação, a unidade é reconhecida pelo documento, nunca pelo nome.
 *      Nome parecido não vira o mesmo cadastro.
 */
@Service
public class CadastroDeUnidades {

    private final ClienteRepositorio unidades;
    private final PagadorRepositorio pagadores;
    private final HistoricoDaUnidadeRepositorio historico;
    private final ContextoEmpresa contexto;

    public CadastroDeUnidades(ClienteRepositorio unidades, PagadorRepositorio pagadores,
                              HistoricoDaUnidadeRepositorio historico,
                              ContextoEmpresa contexto) {
        this.unidades = unidades;
        this.pagadores = pagadores;
        this.historico = historico;
        this.contexto = contexto;
    }

    /** As pessoas da empresa, para escolher o novo dono de uma unidade. */
    public List<Pagador> pessoas() {
        return pagadores.findByEmpresaIdAndAtivoTrueOrderByNome(contexto.exigirEmpresaId());
    }

    public List<HistoricoDaUnidade> trocasDe(UUID unidadeId) {
        return historico.findByUnidadeIdOrderByQuandoDesc(unidadeId);
    }

    /** Os dados comerciais da unidade. Mudança de percentual fica registrada. */
    @Transactional
    public void salvarComercial(UUID unidadeId, BigDecimal percentual, String plataforma,
                                String tomDeCobranca, boolean aceitaParcelamento,
                                LocalDate inicioNaCasa, String motivo) {
        ClienteEspelho unidade = unidade(unidadeId);
        BigDecimal antes = unidade.getPercentual();
        unidade.ajustarComercial(percentual, plataforma, tomDeCobranca, aceitaParcelamento,
                inicioNaCasa);

        if (antes != null && percentual != null && antes.compareTo(percentual) != 0) {
            anotar(unidade, "PERCENTUAL", texto(antes), texto(percentual), motivo);
        }
    }

    /**
     * Passa a unidade para outra pessoa.
     *
     * A dívida continua sendo da unidade. O que muda é quem responde por ela
     * daqui para a frente, e a troca fica registrada com o motivo.
     */
    @Transactional
    public void trocarDono(UUID unidadeId, UUID novoPagadorId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Trocar o dono da unidade exige motivo.");
        }
        ClienteEspelho unidade = unidade(unidadeId);
        Pagador novo = pagadores.findByIdAndEmpresaId(novoPagadorId, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada."));
        String antes = unidade.getPagador() == null ? null : unidade.getPagador().getNome();
        if (unidade.getPagador() != null && unidade.getPagador().getId().equals(novoPagadorId)) {
            throw new IllegalStateException("Esta unidade já é desta pessoa.");
        }
        unidade.vincularA(novo);
        anotar(unidade, "DONO", antes, novo.getNome(), motivo);
    }

    @Transactional
    public void anotarTrocaDeTelefone(UUID unidadeId, String antes, String depois) {
        if (antes != null && !antes.equals(depois)) {
            anotar(unidade(unidadeId), "TELEFONE", antes, depois, null);
        }
    }

    /**
     * Importa a base de clientes de um arquivo.
     *
     * Formato por linha: nome da pessoa; CPF; razão social da unidade; CNPJ;
     * WhatsApp. Pessoa e unidade são reconhecidas pelo documento: quem já
     * existe é reaproveitado, e nada vira cadastro repetido.
     */
    @Transactional
    public Importacao importarBase(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Escolha o arquivo da base.");
        }
        int pessoasNovas = 0;
        int unidadesNovas = 0;
        int jaExistiam = 0;
        List<String> recusadas = new ArrayList<>();

        try (BufferedReader leitor = new BufferedReader(
                new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8))) {
            String linha;
            int numero = 0;
            while ((linha = leitor.readLine()) != null) {
                numero = numero + 1;
                if (linha.isBlank()) {
                    continue;
                }
                String[] partes = linha.split(";");
                if (partes.length < 2) {
                    recusadas.add("linha " + numero + ": precisa de nome e CPF");
                    continue;
                }
                String nome = partes[0].trim();
                String cpf = partes[1].trim();
                if (soNumeros(cpf).isEmpty()) {
                    // Cabecalho do arquivo: passa batido.
                    continue;
                }

                Optional<Pagador> achada = pagadores
                        .findByEmpresaIdAndAtivoTrueOrderByNome(contexto.exigirEmpresaId())
                        .stream()
                        .filter(p -> soNumeros(p.getCpf()).equals(soNumeros(cpf)))
                        .findFirst();
                Pagador pessoa;
                if (achada.isPresent()) {
                    pessoa = achada.get();
                    jaExistiam = jaExistiam + 1;
                } else {
                    pessoa = pagadores.save(new Pagador(contexto.exigirEmpresa(), nome, cpf,
                            contexto.autor()));
                    pessoasNovas = pessoasNovas + 1;
                }

                if (partes.length >= 4 && !soNumeros(partes[3]).isEmpty()) {
                    String razao = partes[2].trim();
                    String cnpj = partes[3].trim();
                    String whatsapp = partes.length >= 5 ? partes[4].trim() : null;

                    boolean unidadeExiste = unidades
                            .findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(
                                    contexto.exigirEmpresaId()).stream()
                            .anyMatch(u -> soNumeros(u.getCnpjCpf()).equals(soNumeros(cnpj)));
                    if (!unidadeExiste) {
                        ClienteEspelho unidade = new ClienteEspelho(contexto.exigirEmpresa(),
                                null, razao.isBlank() ? nome : razao);
                        unidade.atualizarCom(razao.isBlank() ? nome : razao, cnpj, nome,
                                whatsapp, null, true);
                        unidade.vincularA(pessoa);
                        unidades.save(unidade);
                        unidadesNovas = unidadesNovas + 1;
                    }
                }
            }
        } catch (IOException erro) {
            throw new IllegalStateException("Não consegui ler o arquivo: " + erro.getMessage());
        }
        return new Importacao(pessoasNovas, unidadesNovas, jaExistiam, recusadas);
    }

    // ------------------------------------------------------------------ apoio

    private ClienteEspelho unidade(UUID unidadeId) {
        return unidades.findByIdAndEmpresaId(unidadeId, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada."));
    }

    private void anotar(ClienteEspelho unidade, String tipo, String de, String para,
                        String motivo) {
        historico.save(new HistoricoDaUnidade(contexto.exigirEmpresaId(), unidade.getId(),
                tipo, de, para, motivo, contexto.autor()));
    }

    private String texto(BigDecimal valor) {
        return valor == null ? null : valor.stripTrailingZeros().toPlainString();
    }

    private String soNumeros(String texto) {
        return texto == null ? "" : texto.replaceAll("[^0-9]", "");
    }

    /** O resultado da importação, para a tela contar em uma linha. */
    public record Importacao(int pessoasNovas, int unidadesNovas, int jaExistiam,
                             List<String> recusadas) {
    }
}
