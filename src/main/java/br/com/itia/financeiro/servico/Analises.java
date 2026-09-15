package br.com.itia.financeiro.servico;

import br.com.itia.financeiro.dominio.Pagamento;
import br.com.itia.financeiro.dominio.SituacaoTitulo;
import br.com.itia.financeiro.dominio.Titulo;
import br.com.itia.financeiro.repositorio.TituloRepositorio;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * O livro em forma de análise: o que foi cobrado, o que entrou, o que falta.
 *
 * Nada aqui é guardado. Todo número sai dos títulos e dos pagamentos na hora,
 * e por isso pode ser conferido linha a linha na tela de contas a receber.
 */
@Service
public class Analises {

    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yyyy");

    private final TituloRepositorio titulos;
    private final ContextoEmpresa contexto;

    public Analises(TituloRepositorio titulos, ContextoEmpresa contexto) {
        this.titulos = titulos;
        this.contexto = contexto;
    }

    private List<Titulo> daEmpresa() {
        return titulos.findByEmpresaIdOrderByVencimentoDesc(contexto.exigirEmpresaId());
    }

    /**
     * A curva de recuperação: mês a mês, quanto foi cobrado e quanto entrou.
     *
     * O cobrado é contado pela competência do título; o recebido, pela data em
     * que o dinheiro entrou. São critérios diferentes de propósito, e a tela
     * diz isso, para ninguém somar laranja com maçã.
     */
    public List<LinhaDoMes> curvaDeRecuperacao(int meses) {
        YearMonth agora = YearMonth.now();
        Map<String, BigDecimal[]> soma = new LinkedHashMap<>();
        for (int i = meses - 1; i >= 0; i--) {
            soma.put(agora.minusMonths(i).atDay(1).format(MES),
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (Titulo titulo : daEmpresa()) {
            if (!titulo.contaNoTotal()) {
                continue;
            }
            if (titulo.getCompetencia() != null) {
                String mes = titulo.getCompetencia().format(MES);
                BigDecimal[] valores = soma.get(mes);
                if (valores != null) {
                    valores[0] = valores[0].add(titulo.getValor());
                }
            }
            for (Pagamento pagamento : titulo.getPagamentos()) {
                String mes = pagamento.getPagoEm().format(MES);
                BigDecimal[] valores = soma.get(mes);
                if (valores != null) {
                    valores[1] = valores[1].add(pagamento.getValor());
                }
            }
        }

        List<LinhaDoMes> linhas = new ArrayList<>();
        soma.forEach((mes, valores) -> linhas.add(new LinhaDoMes(mes, valores[0], valores[1])));
        return linhas;
    }

    /**
     * A previsão de entrada: o que vence nos próximos meses e ainda tem saldo.
     *
     * A estimativa usa a taxa de conversão dos últimos doze meses. É previsão,
     * e a tela deixa claro que é, com a taxa que foi usada escrita ao lado.
     */
    public Previsao previsao(int meses) {
        LocalDate hoje = LocalDate.now();
        YearMonth agora = YearMonth.from(hoje);
        Map<String, BigDecimal> porMes = new LinkedHashMap<>();
        for (int i = 0; i < meses; i++) {
            porMes.put(agora.plusMonths(i).atDay(1).format(MES), BigDecimal.ZERO);
        }
        BigDecimal vencidoSemPrevisao = BigDecimal.ZERO;

        for (Titulo titulo : daEmpresa()) {
            if (!titulo.contaNoTotal()
                    || titulo.getSaldo().signum() <= 0 || titulo.getVencimento() == null) {
                continue;
            }
            if (titulo.getVencimento().isBefore(hoje)) {
                vencidoSemPrevisao = vencidoSemPrevisao.add(titulo.getSaldo());
                continue;
            }
            String mes = titulo.getVencimento().format(MES);
            porMes.computeIfPresent(mes, (chave, valor) -> valor.add(titulo.getSaldo()));
        }

        BigDecimal taxa = taxaDeConversao();
        List<LinhaDoMes> linhas = new ArrayList<>();
        porMes.forEach((mes, aVencer) -> linhas.add(new LinhaDoMes(mes, aVencer,
                aVencer.multiply(taxa).setScale(2, RoundingMode.HALF_UP))));
        return new Previsao(linhas, taxa, vencidoSemPrevisao);
    }

    /** Quanto do que foi cobrado historicamente virou dinheiro na conta. */
    public BigDecimal taxaDeConversao() {
        BigDecimal cobrado = BigDecimal.ZERO;
        BigDecimal recebido = BigDecimal.ZERO;
        for (Titulo titulo : daEmpresa()) {
            if (!titulo.contaNoTotal()) {
                continue;
            }
            cobrado = cobrado.add(titulo.getValor());
            recebido = recebido.add(titulo.getTotalPago());
        }
        if (cobrado.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return recebido.divide(cobrado, 4, RoundingMode.HALF_UP);
    }

    /**
     * O ranking de risco: quem deve mais, e quanto do rombo está em cada um.
     *
     * A concentração é a conta que diz se o problema está espalhado ou em
     * poucos clientes, que é o que muda a forma de trabalhar a cobrança.
     */
    public List<LinhaDeRisco> ranking(int quantos) {
        LocalDate hoje = LocalDate.now();
        Map<String, BigDecimal[]> porCliente = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Titulo titulo : daEmpresa()) {
            if (!titulo.contaNoTotal()
                    || titulo.getSaldo().signum() <= 0) {
                continue;
            }
            String cliente = titulo.getCliente().getRazaoSocial();
            BigDecimal[] valores = porCliente.computeIfAbsent(cliente,
                    chave -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            valores[0] = valores[0].add(titulo.getSaldo());
            if (titulo.getVencimento() != null && titulo.getVencimento().isBefore(hoje)) {
                valores[1] = valores[1].add(titulo.getSaldo());
                long atraso = java.time.temporal.ChronoUnit.DAYS.between(
                        titulo.getVencimento(), hoje);
                valores[2] = valores[2].max(BigDecimal.valueOf(atraso));
            }
            total = total.add(titulo.getSaldo());
        }

        BigDecimal soma = total;
        List<LinhaDeRisco> linhas = new ArrayList<>();
        porCliente.forEach((cliente, valores) -> linhas.add(new LinhaDeRisco(cliente, valores[0],
                valores[1], valores[2].intValue(),
                soma.signum() == 0 ? BigDecimal.ZERO
                        : valores[0].multiply(new BigDecimal("100"))
                        .divide(soma, 1, RoundingMode.HALF_UP))));
        linhas.sort((a, b) -> b.emAberto().compareTo(a.emAberto()));
        return linhas.size() > quantos ? linhas.subList(0, quantos) : linhas;
    }

    /** Quanto do total em aberto está nos maiores devedores da lista. */
    public BigDecimal concentracao(List<LinhaDeRisco> ranking) {
        return ranking.stream().map(LinhaDeRisco::participacao)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * O arquivo para a contabilidade: uma linha por título, com o que foi pago.
     *
     * Ponto e vírgula como separador e vírgula no decimal, que é o que a
     * planilha em português abre sem perguntar nada.
     */
    public String exportarTitulos() {
        StringBuilder csv = new StringBuilder(
                "numero;cliente;documento;competencia;descricao;vencimento;valor;pago;saldo;"
                        + "situacao;identificador\n");
        for (Titulo titulo : daEmpresa()) {
            csv.append(titulo.getNumero()).append(';')
                    .append(limpar(titulo.getCliente().getRazaoSocial())).append(';')
                    .append(limpar(titulo.getCliente().getCnpjCpf())).append(';')
                    .append(titulo.getCompetencia() == null ? "" : titulo.getCompetencia()).append(';')
                    .append(limpar(titulo.getDescricao())).append(';')
                    .append(titulo.getVencimento() == null ? "" : titulo.getVencimento()).append(';')
                    .append(numero(titulo.getValor())).append(';')
                    .append(numero(titulo.getTotalPago())).append(';')
                    .append(numero(titulo.getSaldo())).append(';')
                    .append(titulo.getSituacao()).append(';')
                    .append(titulo.getIdentificadorPix()).append('\n');
        }
        return csv.toString();
    }

    private String limpar(String texto) {
        return texto == null ? "" : texto.replace(';', ',').replace('\n', ' ');
    }

    private String numero(BigDecimal valor) {
        return valor == null ? "0,00" : valor.setScale(2, RoundingMode.HALF_UP)
                .toPlainString().replace('.', ',');
    }

    /** Um mês da curva: o que foi cobrado e o que entrou. */
    public record LinhaDoMes(String mes, BigDecimal cobrado, BigDecimal recebido) {

        public BigDecimal diferenca() {
            return cobrado.subtract(recebido);
        }

        /** Quanto do cobrado daquele mês virou dinheiro, em porcento. */
        public BigDecimal conversao() {
            if (cobrado.signum() <= 0) {
                return BigDecimal.ZERO;
            }
            return recebido.multiply(new BigDecimal("100"))
                    .divide(cobrado, 1, RoundingMode.HALF_UP);
        }
    }

    /** A previsão de entrada e a taxa usada para estimar. */
    public record Previsao(List<LinhaDoMes> meses, BigDecimal taxa, BigDecimal vencido) {

        public BigDecimal taxaEmPorcento() {
            return taxa.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP);
        }

        public BigDecimal totalAVencer() {
            return meses.stream().map(LinhaDoMes::cobrado).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public BigDecimal totalEstimado() {
            return meses.stream().map(LinhaDoMes::recebido)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /** Uma linha do ranking de risco. */
    public record LinhaDeRisco(String cliente, BigDecimal emAberto, BigDecimal vencido,
                               int maiorAtraso, BigDecimal participacao) {
    }
}
