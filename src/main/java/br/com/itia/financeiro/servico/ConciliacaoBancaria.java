package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.ImportacaoDeExtrato;
import br.com.itia.financeiro.dominio.MovimentoBancario;
import br.com.itia.financeiro.dominio.Obrigacao;
import br.com.itia.financeiro.dominio.Pagamento;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.repositorio.ImportacaoDeExtratoRepositorio;
import br.com.itia.financeiro.repositorio.MovimentoBancarioRepositorio;
import br.com.itia.financeiro.repositorio.ObrigacaoRepositorio;
import br.com.itia.financeiro.repositorio.PagamentoRepositorio;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A conciliação bancária: o extrato de um lado, o sistema do outro.
 *
 * As regras que este serviço protege:
 *   1. O mesmo arquivo não entra duas vezes. A conferência é pelo conteúdo,
 *      não pelo nome: renomear não engana.
 *   2. O mesmo movimento não entra duas vezes: o identificador do banco é
 *      único por empresa.
 *   3. Movimento sem par NÃO recebe classificação chutada. Fica pendente até
 *      alguém decidir, e o motivo de ignorar fica escrito.
 *   4. Conciliar liga o que já existe dos dois lados. Dar baixa é outra coisa,
 *      e acontece com clique, nunca por adivinhação de valor parecido.
 */
@Service
public class ConciliacaoBancaria {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MovimentoBancarioRepositorio movimentos;
    private final ImportacaoDeExtratoRepositorio importacoes;
    private final TituloRepositorio titulos;
    private final PagamentoRepositorio pagamentos;
    private final ObrigacaoRepositorio obrigacoes;
    private final FinanceiroServico financeiro;
    private final ContextoEmpresa contexto;

    public ConciliacaoBancaria(MovimentoBancarioRepositorio movimentos,
                               ImportacaoDeExtratoRepositorio importacoes,
                               TituloRepositorio titulos, PagamentoRepositorio pagamentos,
                               ObrigacaoRepositorio obrigacoes, FinanceiroServico financeiro,
                               ContextoEmpresa contexto) {
        this.movimentos = movimentos;
        this.importacoes = importacoes;
        this.titulos = titulos;
        this.pagamentos = pagamentos;
        this.obrigacoes = obrigacoes;
        this.financeiro = financeiro;
        this.contexto = contexto;
    }

    // ------------------------------------------------------------------ lista

    public List<MovimentoBancario> todos() {
        return movimentos.findByEmpresaIdOrderByOcorridoEmDesc(contexto.exigirEmpresaId());
    }

    public List<MovimentoBancario> pendentes() {
        return todos().stream().filter(MovimentoBancario::pendente).toList();
    }

    public List<ImportacaoDeExtrato> ultimasImportacoes() {
        return importacoes.findTop20ByEmpresaIdOrderByQuandoDesc(contexto.exigirEmpresaId());
    }

    public MovimentoBancario movimento(UUID id) {
        return movimentos.findByIdAndEmpresaId(id, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Movimento não encontrado nesta empresa."));
    }

    /** Os títulos que ainda esperam dinheiro, para a pessoa escolher o par. */
    public List<Titulo> titulosEmAberto() {
        return titulos.findByEmpresaIdAndSituacaoInOrderByVencimento(contexto.exigirEmpresaId(),
                List.of(SituacaoTitulo.ABERTO, SituacaoTitulo.PARCIAL));
    }

    /** As contas a pagar que ainda têm saldo, para o par de uma saída. */
    public List<Obrigacao> contasEmAberto() {
        return obrigacoes.findByEmpresaIdOrderByVencimento(contexto.exigirEmpresaId()).stream()
                .filter(o -> !o.isCancelada() && o.getSaldo().signum() > 0)
                .toList();
    }

    // -------------------------------------------------------------- importar

    /**
     * Lê o extrato de um arquivo de texto.
     *
     * Formato por linha: data; valor; descrição; identificador. Valor negativo
     * é saída; positivo é entrada. O identificador é o que o banco usa para
     * aquela transação, e é ele que impede o movimento entrar duas vezes.
     */
    @Transactional
    public Resultado importar(MultipartFile arquivo, UUID contaId) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Escolha o arquivo do extrato.");
        }
        UUID empresaId = contexto.exigirEmpresaId();
        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException erro) {
            throw new IllegalStateException("Não consegui ler o arquivo: " + erro.getMessage());
        }

        String impressao = impressaoDigital(conteudo);
        importacoes.findByEmpresaIdAndImpressao(empresaId, impressao).ifPresent(antiga -> {
            throw new IllegalStateException("Este extrato já foi importado em "
                    + antiga.getQuando().toLocalDate().format(BR)
                    + " (arquivo " + antiga.getArquivo() + ").");
        });

        ImportacaoDeExtrato importacao = importacoes.save(new ImportacaoDeExtrato(empresaId,
                contaId, arquivo.getOriginalFilename(), impressao, contexto.autor()));

        int lidas = 0;
        int novos = 0;
        int repetidos = 0;
        List<String> recusadas = new ArrayList<>();

        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(conteudo), StandardCharsets.UTF_8))) {
            String linha;
            int numero = 0;
            while ((linha = leitor.readLine()) != null) {
                numero = numero + 1;
                if (linha.isBlank()) {
                    continue;
                }
                String[] partes = linha.split(";");
                if (partes.length < 2) {
                    recusadas.add("linha " + numero + ": precisa de data e valor");
                    continue;
                }
                LocalDate dia = data(partes[0].trim());
                if (dia == null) {
                    // Cabecalho do arquivo: passa batido, sem reclamar.
                    continue;
                }
                BigDecimal valor = numero(partes[1]);
                if (valor == null) {
                    recusadas.add("linha " + numero + ": valor não entendido");
                    continue;
                }
                lidas = lidas + 1;

                String descricao = partes.length >= 3 ? partes[2].trim() : null;
                String identificador = partes.length >= 4 && !partes[3].isBlank()
                        ? partes[3].trim() : null;

                if (identificador != null && movimentos
                        .findByEmpresaIdAndIdentificador(empresaId, identificador).isPresent()) {
                    repetidos = repetidos + 1;
                    continue;
                }

                movimentos.save(new MovimentoBancario(empresaId, contaId, importacao.getId(), dia,
                        valor.abs(), valor.signum() < 0 ? "DEBITO" : "CREDITO", descricao,
                        identificador));
                novos = novos + 1;
            }
        } catch (IOException erro) {
            throw new IllegalStateException("Não consegui ler o arquivo: " + erro.getMessage());
        }

        importacao.contar(lidas, novos, repetidos);
        importacoes.save(importacao);
        return new Resultado(lidas, novos, repetidos, recusadas, casarSozinho());
    }

    // -------------------------------------------------------- casar sozinho

    /**
     * Liga o que dá para ligar sem dúvida: pelo identificador.
     *
     * Só casa quando o identificador do extrato bate com o identificador da
     * cobrança ou com o identificador de um pagamento já lançado. Valor
     * parecido em data parecida NÃO é motivo para casar nada.
     */
    @Transactional
    public int casarSozinho() {
        UUID empresaId = contexto.exigirEmpresaId();
        int casados = 0;

        for (MovimentoBancario movimento : pendentes()) {
            if (movimento.getIdentificador() == null) {
                continue;
            }
            Optional<Titulo> porCobranca = titulos.findByEmpresaIdAndIdentificadorPix(empresaId,
                    movimento.getIdentificador());
            if (porCobranca.isPresent()) {
                movimento.conciliarComTitulo(porCobranca.get().getId(), "conciliação automática");
                movimentos.save(movimento);
                casados = casados + 1;
                continue;
            }

            Optional<Pagamento> porTransacao = pagamentos
                    .findByEmpresaIdAndTransacaoId(empresaId, movimento.getIdentificador());
            if (porTransacao.isPresent()) {
                movimento.conciliarComTitulo(porTransacao.get().getTitulo().getId(),
                        "conciliação automática");
                movimentos.save(movimento);
                casados = casados + 1;
            }
        }
        return casados;
    }

    // ------------------------------------------------------------ na mão

    /** Liga o movimento a um título e, se pedirem, dá a baixa junto. */
    @Transactional
    public void ligarAoTitulo(UUID movimentoId, UUID tituloId, boolean darBaixa) {
        MovimentoBancario movimento = movimento(movimentoId);
        Titulo titulo = titulos.findByIdAndEmpresaId(tituloId, contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Título não encontrado."));

        if (darBaixa && titulo.getSaldo().signum() > 0) {
            BigDecimal valor = movimento.getValor().min(titulo.getSaldo());
            financeiro.receberPagamento(titulo.getId(), valor, movimento.getOcorridoEm(),
                    "EXTRATO", movimento.getIdentificador());
        }
        movimento.conciliarComTitulo(titulo.getId(), contexto.autor());
        movimentos.save(movimento);
    }

    /** Liga o movimento a uma conta a pagar, e opcionalmente registra o pagamento. */
    @Transactional
    public void ligarAConta(UUID movimentoId, UUID obrigacaoId, boolean registrarPagamento) {
        MovimentoBancario movimento = movimento(movimentoId);
        Obrigacao obrigacao = obrigacoes.findByIdAndEmpresaId(obrigacaoId,
                        contexto.exigirEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));

        if (registrarPagamento && obrigacao.getSaldo().signum() > 0) {
            BigDecimal valor = movimento.getValor().min(obrigacao.getSaldo());
            br.com.itia.financeiro.dominio.PagamentoDeObrigacao pagamento =
                    new br.com.itia.financeiro.dominio.PagamentoDeObrigacao(obrigacao,
                            movimento.getOcorridoEm(), valor, null, "EXTRATO", contexto.autor());
            obrigacao.receber(pagamento);
            obrigacoes.save(obrigacao);
        }
        obrigacao.ajustarConciliacao(br.com.itia.financeiro.dominio.Conciliacao.CONCILIADA);
        movimento.conciliarComConta(obrigacao.getId(), contexto.autor());
        movimentos.save(movimento);
    }

    @Transactional
    public void ignorar(UUID movimentoId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "Ignorar um movimento exige motivo: é ele que explica o buraco depois.");
        }
        MovimentoBancario movimento = movimento(movimentoId);
        movimento.ignorar(motivo, contexto.autor());
        movimentos.save(movimento);
    }

    @Transactional
    public void desfazer(UUID movimentoId) {
        MovimentoBancario movimento = movimento(movimentoId);
        movimento.voltarParaPendente();
        movimentos.save(movimento);
    }

    // ------------------------------------------------------ fechamento do dia

    /**
     * O fechamento do dia: o que o banco moveu contra o que o sistema registrou.
     *
     * Enquanto a diferença não for zero, alguma linha do extrato não tem par, e
     * é isso que a tela mostra.
     */
    public List<LinhaDoDia> fechamentoDiario(LocalDate de, LocalDate ate) {
        List<LinhaDoDia> dias = new ArrayList<>();
        var lista = movimentos.findByEmpresaIdAndOcorridoEmBetweenOrderByOcorridoEm(
                contexto.exigirEmpresaId(), de, ate);

        LocalDate dia = de;
        while (!dia.isAfter(ate)) {
            LocalDate hoje = dia;
            var doDia = lista.stream().filter(m -> m.getOcorridoEm().equals(hoje)).toList();
            BigDecimal entradas = doDia.stream().filter(MovimentoBancario::entrou)
                    .map(MovimentoBancario::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal saidas = doDia.stream().filter(m -> !m.entrou())
                    .map(MovimentoBancario::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
            long pendentes = doDia.stream().filter(MovimentoBancario::pendente).count();
            BigDecimal semPar = doDia.stream().filter(MovimentoBancario::pendente)
                    .map(MovimentoBancario::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

            if (!doDia.isEmpty()) {
                dias.add(new LinhaDoDia(hoje, entradas, saidas, (int) pendentes, semPar));
            }
            dia = dia.plusDays(1);
        }
        return dias;
    }

    // ------------------------------------------------------------------ apoio

    private String impressaoDigital(byte[] conteudo) {
        try {
            MessageDigest algoritmo = MessageDigest.getInstance("SHA-256");
            StringBuilder texto = new StringBuilder();
            for (byte parte : algoritmo.digest(conteudo)) {
                texto.append(String.format("%02x", parte));
            }
            return texto.toString();
        } catch (NoSuchAlgorithmException impossivel) {
            throw new IllegalStateException("Não consegui conferir o arquivo.");
        }
    }

    private LocalDate data(String bruto) {
        String limpo = bruto.replace("\"", "").trim();
        try {
            return limpo.contains("/") ? LocalDate.parse(limpo, BR) : LocalDate.parse(limpo);
        } catch (RuntimeException naoEData) {
            return null;
        }
    }

    private BigDecimal numero(String bruto) {
        String limpo = bruto.replace("\"", "").replace("R$", "").trim()
                .replace(".", "").replace(",", ".");
        if (limpo.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(limpo);
        } catch (NumberFormatException naoENumero) {
            return null;
        }
    }

    /** O resultado da importação, para a tela contar em uma linha. */
    public record Resultado(int lidas, int novos, int repetidos, List<String> recusadas,
                            int casadosSozinho) {
    }

    /** Um dia do fechamento: o que entrou, o que saiu e o que não tem par. */
    public record LinhaDoDia(LocalDate dia, BigDecimal entradas, BigDecimal saidas,
                             int pendentes, BigDecimal semPar) {
        public boolean fecha() {
            return pendentes == 0;
        }
    }
}
