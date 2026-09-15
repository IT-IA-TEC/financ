package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.ClienteEspelho;
import br.com.itia.financeiro.dominio.Cobranca;
import br.com.itia.financeiro.dominio.Competencia;
import br.com.itia.financeiro.dominio.ContratacaoDePacote;
import br.com.itia.financeiro.dominio.FaturadoDoPeriodo;
import br.com.itia.financeiro.dominio.Pagador;
import br.com.itia.financeiro.dominio.SituacaoDaContratacao;
import br.com.itia.financeiro.repositorio.ClienteRepositorio;
import br.com.itia.financeiro.repositorio.CobrancaRepositorio;
import br.com.itia.financeiro.repositorio.CompetenciaRepositorio;
import br.com.itia.financeiro.repositorio.ContratacaoDePacoteRepositorio;
import br.com.itia.financeiro.repositorio.FaturadoDoPeriodoRepositorio;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O fechamento do mês: a leva de cobranças de um período.
 *
 * As regras que este serviço protege:
 *   1. Nada sai antes de alguém ver a prévia. A prévia mostra o que vai ser
 *      cobrado E o que vai ficar de fora, com o motivo de cada um.
 *   2. Competência fechada não gera nem recalcula. Para mexer, reabre de
 *      propósito, e o nome de quem reabriu fica gravado.
 *   3. O valor que vem de fora manda no valor da leva, e fica guardado com a
 *      origem do arquivo. Linha de arquivo sem dono não vira cobrança de
 *      ninguém: aparece na lista do que ficou de fora.
 */
@Service
public class FechamentoDoMes {

    private static final DateTimeFormatter REFERENCIA = DateTimeFormatter.ofPattern("MM/yyyy");

    private final ContratacaoDePacoteRepositorio contratacoes;
    private final CobrancaRepositorio cobrancas;
    private final CompetenciaRepositorio competencias;
    private final FaturadoDoPeriodoRepositorio faturados;
    private final PagadorRepositorio pagadores;
    private final ClienteRepositorio unidades;
    private final ContextoEmpresa contexto;

    public FechamentoDoMes(ContratacaoDePacoteRepositorio contratacoes,
                           CobrancaRepositorio cobrancas, CompetenciaRepositorio competencias,
                           FaturadoDoPeriodoRepositorio faturados, PagadorRepositorio pagadores,
                           ClienteRepositorio unidades, ContextoEmpresa contexto) {
        this.contratacoes = contratacoes;
        this.cobrancas = cobrancas;
        this.competencias = competencias;
        this.faturados = faturados;
        this.pagadores = pagadores;
        this.unidades = unidades;
        this.contexto = contexto;
    }

    // ------------------------------------------------------------------ prévia

    /**
     * O que vai acontecer se a leva for gerada agora.
     *
     * Não grava nada. Cada contratação vira uma linha, com a situação dela:
     * pronta, já gerada, sem valor, fora da vigência ou pacote inativo.
     */
    public Previa previa(YearMonth periodo) {
        UUID empresaId = contexto.exigirEmpresaId();
        LocalDate inicio = periodo.atDay(1);
        LocalDate fim = periodo.atEndOfMonth();
        String referencia = inicio.format(REFERENCIA);

        Map<String, BigDecimal> deFora = faturadoPorDocumento(empresaId, referencia);
        List<Linha> linhas = new ArrayList<>();

        for (ContratacaoDePacote c : contratacoes.findByEmpresaIdOrderByCriadoEmDesc(empresaId)) {
            String cliente = c.getPagador().getNome();
            String pacote = c.getPacote().getNome();
            String unidade = c.getUnidade() == null ? "todas as unidades"
                    : c.getUnidade().getRazaoSocial();

            if (c.getSituacao() != SituacaoDaContratacao.ATIVA) {
                linhas.add(Linha.fora(cliente, pacote, unidade,
                        "contratação " + c.getSituacao().getRotulo()));
                continue;
            }
            if (!alcanca(c, inicio, fim)) {
                linhas.add(Linha.fora(cliente, pacote, unidade, "fora da vigência neste mês"));
                continue;
            }
            if (!c.getPacote().isAtivo()) {
                linhas.add(Linha.fora(cliente, pacote, unidade, "pacote inativo"));
                continue;
            }
            if (cobrancas.findByContratacaoIdAndReferencia(c.getId(), referencia).isPresent()) {
                linhas.add(Linha.fora(cliente, pacote, unidade, "já tem cobrança neste mês"));
                continue;
            }

            BigDecimal valor = valorDaContratacao(c, deFora);
            if (valor == null || valor.signum() <= 0) {
                linhas.add(Linha.fora(cliente, pacote, unidade, "sem valor definido"));
                continue;
            }
            linhas.add(Linha.pronta(c.getId(), cliente, pacote, unidade, valor,
                    veioDeFora(c, deFora)));
        }

        List<FaturadoDoPeriodo> orfaos = faturados
                .findByEmpresaIdAndReferenciaOrderByDocumento(empresaId, referencia).stream()
                .filter(FaturadoDoPeriodo::semDono)
                .toList();

        return new Previa(referencia, linhas, orfaos, competencia(referencia));
    }

    /** O valor desta contratação neste mês: o que veio de fora manda. */
    private BigDecimal valorDaContratacao(ContratacaoDePacote c, Map<String, BigDecimal> deFora) {
        BigDecimal informado = valorDeFora(c, deFora);
        return informado != null ? informado : c.getValorEfetivo();
    }

    private boolean veioDeFora(ContratacaoDePacote c, Map<String, BigDecimal> deFora) {
        return valorDeFora(c, deFora) != null;
    }

    private BigDecimal valorDeFora(ContratacaoDePacote c, Map<String, BigDecimal> deFora) {
        if (deFora.isEmpty()) {
            return null;
        }
        if (c.getUnidade() != null) {
            BigDecimal daUnidade = deFora.get(soNumeros(c.getUnidade().getCnpjCpf()));
            if (daUnidade != null) {
                return daUnidade;
            }
        }
        return deFora.get(soNumeros(c.getPagador().getCpf()));
    }

    private Map<String, BigDecimal> faturadoPorDocumento(UUID empresaId, String referencia) {
        Map<String, BigDecimal> mapa = new LinkedHashMap<>();
        for (FaturadoDoPeriodo linha : faturados
                .findByEmpresaIdAndReferenciaOrderByDocumento(empresaId, referencia)) {
            mapa.merge(soNumeros(linha.getDocumento()), linha.getValor(), BigDecimal::add);
        }
        return mapa;
    }

    private boolean alcanca(ContratacaoDePacote c, LocalDate inicio, LocalDate fim) {
        boolean jaComecou = c.getInicio() == null || !c.getInicio().isAfter(fim);
        boolean aindaNaoAcabou = c.getFim() == null || !c.getFim().isBefore(inicio);
        return jaComecou && aindaNaoAcabou;
    }

    // ------------------------------------------------------------------ geração

    /**
     * Gera a leva do período, a partir da prévia.
     *
     * Roda quantas vezes quiser: quem já tem cobrança no mês é pulado, e por
     * isso ninguém cobra o mesmo mês duas vezes.
     */
    @Transactional
    public int gerar(YearMonth periodo, int diaDoVencimento) {
        String referencia = periodo.atDay(1).format(REFERENCIA);
        exigirAberta(referencia);

        LocalDate inicio = periodo.atDay(1);
        LocalDate fim = periodo.atEndOfMonth();
        Previa previa = previa(periodo);
        int criadas = 0;

        for (Linha linha : previa.prontas()) {
            ContratacaoDePacote c = contratacoes.findById(linha.contratacaoId()).orElse(null);
            if (c == null) {
                continue;
            }
            Cobranca cobranca = new Cobranca(c.getPacote().getEmpresa(),
                    br.com.itia.financeiro.dominio.OrigemDaCobranca.PACOTE, c.getPagador(),
                    "Pacote " + c.getPacote().getNome() + " · " + referencia, contexto.autor());
            cobranca.vincularOrigem(c, null);
            cobranca.ajustarPeriodo(inicio, fim, referencia);
            cobranca.ajustarDados(cobranca.getDescricao(),
                    fim.withDayOfMonth(Math.min(diaDoVencimento, fim.lengthOfMonth())),
                    c.getUnidade());
            cobranca.ajustarValores(linha.valor(), BigDecimal.ZERO,
                    linha.valorVeioDeFora() ? "valor do arquivo importado" : null);
            cobrancas.save(cobranca);
            criadas = criadas + 1;
        }
        return criadas;
    }

    // ------------------------------------------------------- trava do período

    public Optional<Competencia> competencia(String referencia) {
        return competencias.findByEmpresaIdAndReferencia(contexto.exigirEmpresaId(), referencia);
    }

    public List<Competencia> competencias() {
        return competencias.findByEmpresaIdOrderByReferenciaDesc(contexto.exigirEmpresaId());
    }

    @Transactional
    public void fechar(YearMonth periodo) {
        contexto.exigirPapel(br.com.itia.financeiro.dominio.UsuarioEmpresa.Papel.DIRETOR);
        String referencia = periodo.atDay(1).format(REFERENCIA);
        Competencia competencia = competencia(referencia)
                .orElseGet(() -> competencias.save(
                        new Competencia(contexto.exigirEmpresa(), referencia)));
        if (competencia.estaFechada()) {
            throw new IllegalStateException("Este mês já está fechado.");
        }
        competencia.fechar(contexto.autor());
        competencias.save(competencia);
    }

    @Transactional
    public void reabrir(YearMonth periodo, String motivo) {
        contexto.exigirPapel(br.com.itia.financeiro.dominio.UsuarioEmpresa.Papel.DIRETOR);
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Reabrir um mês fechado exige motivo.");
        }
        String referencia = periodo.atDay(1).format(REFERENCIA);
        Competencia competencia = competencia(referencia)
                .orElseThrow(() -> new IllegalArgumentException("Este mês nunca foi fechado."));
        competencia.reabrir(contexto.autor(), motivo);
        competencias.save(competencia);
    }

    /** Barra qualquer mudança em mês fechado. */
    public void exigirAberta(String referencia) {
        competencia(referencia).filter(Competencia::estaFechada).ifPresent(c -> {
            throw new IllegalStateException("O mês " + referencia + " está fechado desde "
                    + c.getFechadaEm().toLocalDate() + ", por " + c.getFechadaPor()
                    + ". Reabra antes de mexer.");
        });
    }

    // -------------------------------------------------- entrada do faturado

    public List<FaturadoDoPeriodo> faturadoDe(YearMonth periodo) {
        return faturados.findByEmpresaIdAndReferenciaOrderByDocumento(
                contexto.exigirEmpresaId(), periodo.atDay(1).format(REFERENCIA));
    }

    /**
     * Lê o arquivo do faturado do mês.
     *
     * Formato: documento e valor por linha, separados por ponto e vírgula ou
     * vírgula. A primeira linha pode ser o cabeçalho. Cada linha é amarrada ao
     * cliente pelo documento; o que não achar dono fica visível na prévia.
     */
    @Transactional
    public Importacao importar(YearMonth periodo, MultipartFile arquivo, boolean substituir) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Escolha o arquivo do faturado.");
        }
        UUID empresaId = contexto.exigirEmpresaId();
        String referencia = periodo.atDay(1).format(REFERENCIA);
        exigirAberta(referencia);

        if (substituir) {
            faturados.deleteByEmpresaIdAndReferencia(empresaId, referencia);
        }

        List<Pagador> clientes = pagadores.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId);
        List<ClienteEspelho> todasAsUnidades = unidades
                .findByEmpresaIdAndAtivoTrueOrderByRazaoSocial(empresaId);

        int lidas = 0;
        int amarradas = 0;
        int semDono = 0;
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
                String[] partes = separar(linha);
                if (partes == null) {
                    recusadas.add("linha " + numero + ": faltou documento ou valor");
                    continue;
                }
                String documento = partes[0].trim().replace("\"", "");
                String bruto = partes[1].trim().replace("\"", "").replace("R$", "").trim();
                if (documento.isBlank() || soNumeros(documento).isEmpty()) {
                    // Cabecalho ou linha de titulo: passa batido, sem reclamar.
                    continue;
                }
                BigDecimal valor;
                try {
                    valor = new BigDecimal(bruto.replace(".", "").replace(",", "."));
                } catch (NumberFormatException valorIlegivel) {
                    recusadas.add("linha " + numero + ": valor não entendido (" + bruto + ")");
                    continue;
                }

                lidas = lidas + 1;
                FaturadoDoPeriodo registro = new FaturadoDoPeriodo(contexto.exigirEmpresa(),
                        referencia, documento, valor, arquivo.getOriginalFilename(),
                        contexto.autor());

                ClienteEspelho unidade = todasAsUnidades.stream()
                        .filter(u -> soNumeros(u.getCnpjCpf()).equals(soNumeros(documento)))
                        .findFirst().orElse(null);
                Pagador dono = unidade != null && unidade.getPagador() != null
                        ? unidade.getPagador()
                        : clientes.stream()
                        .filter(p -> soNumeros(p.getCpf()).equals(soNumeros(documento)))
                        .findFirst().orElse(null);

                if (dono != null) {
                    registro.amarrarAoCliente(dono, unidade);
                    amarradas = amarradas + 1;
                } else {
                    semDono = semDono + 1;
                }
                faturados.save(registro);
            }
        } catch (IOException erro) {
            throw new IllegalStateException("Não consegui ler o arquivo: " + erro.getMessage());
        }

        return new Importacao(lidas, amarradas, semDono, recusadas);
    }

    /**
     * Corta a linha em documento e valor.
     *
     * Ponto e virgula ou tabulacao mandam. Sem eles, o corte e na ULTIMA
     * virgula: assim 12345678900,777,50 continua sendo documento e valor, e a
     * virgula do centavo nao vira separador de coluna.
     */
    private String[] separar(String linha) {
        if (linha.contains(";")) {
            String[] partes = linha.split(";");
            return partes.length >= 2 ? new String[]{partes[0], partes[1]} : null;
        }
        if (linha.contains("	")) {
            String[] partes = linha.split("	");
            return partes.length >= 2 ? new String[]{partes[0], partes[1]} : null;
        }
        int corte = linha.lastIndexOf(',');
        if (corte <= 0) {
            return null;
        }
        String possivelValor = linha.substring(corte + 1).trim();
        // Virgula de centavo: o corte de verdade e a virgula anterior.
        if (possivelValor.length() <= 2 && possivelValor.matches("[0-9]+")) {
            int anterior = linha.lastIndexOf(',', corte - 1);
            if (anterior <= 0) {
                return null;
            }
            corte = anterior;
        }
        return new String[]{linha.substring(0, corte), linha.substring(corte + 1)};
    }

    private String soNumeros(String texto) {
        return texto == null ? "" : texto.replaceAll("[^0-9]", "");
    }

    // ------------------------------------------------------------- resultados

    /** Uma linha da prévia: o que vai ser cobrado, ou por que não vai. */
    public record Linha(UUID contratacaoId, String cliente, String pacote, String unidade,
                        BigDecimal valor, boolean valorVeioDeFora, String motivoDeFicarDeFora) {

        static Linha pronta(UUID id, String cliente, String pacote, String unidade,
                            BigDecimal valor, boolean deFora) {
            return new Linha(id, cliente, pacote, unidade, valor, deFora, null);
        }

        static Linha fora(String cliente, String pacote, String unidade, String motivo) {
            return new Linha(null, cliente, pacote, unidade, BigDecimal.ZERO, false, motivo);
        }

        public boolean entra() {
            return motivoDeFicarDeFora == null;
        }
    }

    /** A prévia inteira, com os totais que a tela mostra. */
    public record Previa(String referencia, List<Linha> linhas, List<FaturadoDoPeriodo> semDono,
                         Optional<Competencia> competencia) {

        public List<Linha> prontas() {
            return linhas.stream().filter(Linha::entra).toList();
        }

        public List<Linha> foraDaLeva() {
            return linhas.stream().filter(l -> !l.entra()).toList();
        }

        public BigDecimal total() {
            return prontas().stream().map(Linha::valor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public int quantasEntram() {
            return prontas().size();
        }

        public int quantasFicamDeFora() {
            return foraDaLeva().size();
        }

        public boolean fechada() {
            return competencia.map(Competencia::estaFechada).orElse(false);
        }
    }

    /** O resultado da leitura do arquivo, para a tela contar em uma linha. */
    public record Importacao(int lidas, int amarradas, int semDono, List<String> recusadas) {
    }
}
