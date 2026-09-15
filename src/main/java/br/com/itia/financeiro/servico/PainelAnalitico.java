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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Os dois painéis analíticos do contas a receber.
 *
 * O primeiro responde "o dinheiro está entrando?"; o segundo responde "onde
 * está o risco?". São os dois lados que uma área financeira olha todo dia, e
 * cada um tem quatro modos de ver a mesma carteira.
 *
 * Três regras que o desenho protege:
 *   1. Todo número sai dos títulos e dos pagamentos da empresa aberta, na
 *      hora. Nada é guardado nem estimado sem dizer que é estimativa.
 *   2. Cada modo vem com a leitura do resultado: o que aquele desenho quer
 *      dizer PARA ESTA empresa, e não um texto genérico.
 *   3. Indicador sem base suficiente diz que não tem base, em vez de mostrar
 *      um número bonito que ninguém pode usar.
 */
@Service
public class PainelAnalitico {

    private static final DateTimeFormatter MES_CURTO = DateTimeFormatter.ofPattern("MM/yy");

    private final TituloRepositorio titulos;
    private final ContextoEmpresa contexto;

    public PainelAnalitico(TituloRepositorio titulos, ContextoEmpresa contexto) {
        this.titulos = titulos;
        this.contexto = contexto;
    }

    // ------------------------------------------------------------- período

    /** Os períodos que a tela oferece. O mês corrente é o que abre. */
    public List<Periodo> periodos() {
        LocalDate hoje = LocalDate.now();
        YearMonth mes = YearMonth.from(hoje);
        return List.of(
                new Periodo("mes", "Mês corrente", mes.atDay(1), mes.atEndOfMonth()),
                new Periodo("mes_anterior", "Mês anterior", mes.minusMonths(1).atDay(1),
                        mes.minusMonths(1).atEndOfMonth()),
                new Periodo("trimestre", "Últimos 3 meses", mes.minusMonths(2).atDay(1),
                        mes.atEndOfMonth()),
                new Periodo("semestre", "Últimos 6 meses", mes.minusMonths(5).atDay(1),
                        mes.atEndOfMonth()),
                new Periodo("ano", "Ano vigente", LocalDate.of(hoje.getYear(), 1, 1),
                        LocalDate.of(hoje.getYear(), 12, 31)),
                new Periodo("ano_anterior", "Ano anterior",
                        LocalDate.of(hoje.getYear() - 1, 1, 1),
                        LocalDate.of(hoje.getYear() - 1, 12, 31)),
                new Periodo("doze", "Últimos 12 meses", mes.minusMonths(11).atDay(1),
                        mes.atEndOfMonth()),
                new Periodo("tudo", "Tudo", LocalDate.of(2000, 1, 1), hoje.plusYears(50)));
    }

    public Periodo periodo(String chave) {
        return periodos().stream().filter(p -> p.chave().equals(chave)).findFirst()
                .orElse(periodos().get(0));
    }

    // -------------------------------------------------------------- painéis

    public Resposta montar(String chaveDoPeriodo) {
        Periodo periodo = periodo(chaveDoPeriodo);
        List<Titulo> carteira = titulos.findByEmpresaIdOrderByVencimentoDesc(
                contexto.exigirEmpresaId()).stream()
                .filter(Titulo::contaNoTotal)
                .toList();

        return new Resposta(periodo, periodos(),
                new Painel("Saúde do recebimento",
                        "O dinheiro está entrando no ritmo que deveria?",
                        List.of(aging(carteira), cobradoRecebido(carteira, periodo),
                                prazoMedio(carteira, periodo), acumulado(carteira, periodo))),
                new Painel("Risco e concentração",
                        "De quem depende o resultado, e o que pode não entrar?",
                        List.of(pareto(carteira), inadimplenciaPorSafra(carteira, periodo),
                                previsao(carteira), composicao(carteira, periodo))));
    }

    // --------------------------------------------------------------- modo 1

    /**
     * Aging: quanto do que está em aberto já venceu, por faixa de atraso.
     *
     * É o indicador mais antigo do contas a receber, e continua sendo o
     * primeiro que qualquer área financeira olha: dívida velha volta menos.
     */
    private Modo aging(List<Titulo> carteira) {
        LocalDate hoje = LocalDate.now();
        String[] faixas = {"A vencer", "1 a 30", "31 a 60", "61 a 90", "Mais de 90"};
        BigDecimal[] valores = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};

        for (Titulo t : carteira) {
            BigDecimal saldo = t.getSaldo();
            if (saldo.signum() <= 0 || t.getVencimento() == null) {
                continue;
            }
            long dias = ChronoUnit.DAYS.between(t.getVencimento(), hoje);
            int faixa = dias <= 0 ? 0 : dias <= 30 ? 1 : dias <= 60 ? 2 : dias <= 90 ? 3 : 4;
            valores[faixa] = valores[faixa].add(saldo);
        }

        List<Ponto> pontos = new ArrayList<>();
        for (int i = 0; i < faixas.length; i++) {
            pontos.add(new Ponto(faixas[i], valores[i]));
        }
        BigDecimal total = soma(valores);
        BigDecimal vencido = total.subtract(valores[0]);
        BigDecimal velho = valores[3].add(valores[4]);

        String texto;
        String situacao;
        if (total.signum() <= 0) {
            texto = "Não há nada em aberto nesta empresa agora.";
            situacao = "boa";
        } else if (porcento(velho, total).compareTo(new BigDecimal("30")) >= 0) {
            texto = "Mais de 30% do que está em aberto já passou de 60 dias. "
                    + "Dívida dessa idade raramente volta sozinha: ou entra em acordo, "
                    + "ou vira perda. É aqui que a cobrança precisa gastar o tempo dela.";
            situacao = "ruim";
        } else if (vencido.signum() > 0) {
            texto = "Há atraso na carteira, mas concentrado nas faixas curtas. "
                    + "Cobrança rápida nos primeiros 30 dias costuma resolver a maior "
                    + "parte disso sem desgaste com o cliente.";
            situacao = "atencao";
        } else {
            texto = "Nada vencido: tudo que está em aberto ainda tem prazo. "
                    + "O trabalho aqui é de lembrete, não de cobrança.";
            situacao = "boa";
        }

        return new Modo("aging", "Aging da carteira", "barras",
                "Quanto do saldo em aberto está em cada faixa de atraso, hoje.",
                List.of(new Serie("Em aberto", "barra", "tinta", pontos)),
                new Leitura("Idade da dívida",
                        texto,
                        total.signum() <= 0 ? "sem saldo"
                                : porcento(vencido, total) + "% vencido",
                        situacao));
    }

    // --------------------------------------------------------------- modo 2

    /** Cobrado contra recebido, mês a mês, dentro do período escolhido. */
    private Modo cobradoRecebido(List<Titulo> carteira, Periodo periodo) {
        Map<String, BigDecimal[]> meses = mesesDo(periodo);

        for (Titulo t : carteira) {
            if (t.getCompetencia() != null) {
                somar(meses, t.getCompetencia(), 0, t.getValor());
            }
            for (Pagamento p : t.getPagamentos()) {
                somar(meses, p.getPagoEm(), 1, p.getValor());
            }
        }

        List<Ponto> cobrado = new ArrayList<>();
        List<Ponto> recebido = new ArrayList<>();
        meses.forEach((mes, valores) -> {
            cobrado.add(new Ponto(mes, valores[0]));
            recebido.add(new Ponto(mes, valores[1]));
        });

        BigDecimal totalCobrado = somaDe(cobrado);
        BigDecimal totalRecebido = somaDe(recebido);
        BigDecimal conversao = porcento(totalRecebido, totalCobrado);

        String texto;
        String situacao;
        if (totalCobrado.signum() <= 0) {
            texto = "Nada foi cobrado neste período, então não há conversão para medir.";
            situacao = "neutra";
        } else if (conversao.compareTo(new BigDecimal("90")) >= 0) {
            texto = "Quase tudo que foi cobrado no período já entrou. Uma carteira "
                    + "assim aguenta crescer sem apertar o caixa.";
            situacao = "boa";
        } else if (conversao.compareTo(new BigDecimal("70")) >= 0) {
            texto = "A maior parte entrou, mas sobra uma fatia relevante. Vale olhar "
                    + "se o que ficou é de poucos clientes ou está espalhado.";
            situacao = "atencao";
        } else {
            texto = "Menos de 70% do cobrado virou dinheiro no período. Nesse patamar "
                    + "o problema costuma ser de processo de cobrança, e não de "
                    + "capacidade de pagar do cliente.";
            situacao = "ruim";
        }

        return new Modo("cobrado_recebido", "Cobrado x recebido", "barras-agrupadas",
                "O que foi cobrado por competência e o que entrou por data de pagamento.",
                List.of(new Serie("Cobrado", "barra", "tinta", cobrado),
                        new Serie("Recebido", "barra", "acento", recebido)),
                new Leitura("Conversão do período", texto,
                        totalCobrado.signum() <= 0 ? "sem base" : conversao + "% convertido",
                        situacao));
    }

    // --------------------------------------------------------------- modo 3

    /**
     * Prazo médio de recebimento e atraso médio, mês a mês.
     *
     * O prazo conta da competência até o dia do pagamento; o atraso conta do
     * vencimento até o pagamento. Um diz quanto tempo o dinheiro leva para
     * chegar; o outro diz o quanto o cliente estoura o combinado.
     */
    private Modo prazoMedio(List<Titulo> carteira, Periodo periodo) {
        Map<String, BigDecimal[]> meses = mesesDo(periodo);
        Map<String, BigDecimal[]> pesos = mesesDo(periodo);

        for (Titulo t : carteira) {
            for (Pagamento p : t.getPagamentos()) {
                String mes = p.getPagoEm().format(MES_CURTO);
                if (!meses.containsKey(mes)) {
                    continue;
                }
                BigDecimal valor = p.getValor();
                long prazo = t.getCompetencia() == null ? 0
                        : ChronoUnit.DAYS.between(t.getCompetencia(), p.getPagoEm());
                long atraso = t.getVencimento() == null ? 0
                        : Math.max(0, ChronoUnit.DAYS.between(t.getVencimento(), p.getPagoEm()));
                meses.get(mes)[0] = meses.get(mes)[0].add(valor.multiply(BigDecimal.valueOf(prazo)));
                meses.get(mes)[1] = meses.get(mes)[1].add(valor.multiply(BigDecimal.valueOf(atraso)));
                pesos.get(mes)[0] = pesos.get(mes)[0].add(valor);
            }
        }

        List<Ponto> prazo = new ArrayList<>();
        List<Ponto> atraso = new ArrayList<>();
        meses.forEach((mes, valores) -> {
            BigDecimal peso = pesos.get(mes)[0];
            prazo.add(new Ponto(mes, peso.signum() == 0 ? BigDecimal.ZERO
                    : valores[0].divide(peso, 0, RoundingMode.HALF_UP)));
            atraso.add(new Ponto(mes, peso.signum() == 0 ? BigDecimal.ZERO
                    : valores[1].divide(peso, 0, RoundingMode.HALF_UP)));
        });

        BigDecimal atrasoMedio = mediaDe(atraso);
        String texto;
        String situacao;
        if (atrasoMedio.signum() == 0 && somaDe(prazo).signum() == 0) {
            texto = "Ninguém pagou no período, então não há prazo para medir.";
            situacao = "neutra";
        } else if (atrasoMedio.compareTo(new BigDecimal("5")) <= 0) {
            texto = "Quem paga, paga quase no dia. Esse comportamento permite prever "
                    + "caixa com segurança e até oferecer desconto por antecipação.";
            situacao = "boa";
        } else if (atrasoMedio.compareTo(new BigDecimal("15")) <= 0) {
            texto = "O atraso médio está na faixa em que o lembrete antes do vencimento "
                    + "costuma resolver. Cada dia a menos aqui é caixa que entra antes.";
            situacao = "atencao";
        } else {
            texto = "O cliente já trata o vencimento como sugestão. Atraso médio nesse "
                    + "nível exige régua de cobrança com data, e não cobrança avulsa.";
            situacao = "ruim";
        }

        return new Modo("prazo", "Prazo e atraso médios", "linhas",
                "Dias entre a competência e o pagamento, e dias de atraso, por mês.",
                List.of(new Serie("Prazo até o pagamento", "linha", "tinta", prazo),
                        new Serie("Atraso sobre o vencimento", "linha", "acento", atraso)),
                new Leitura("Velocidade do dinheiro", texto,
                        atrasoMedio + " dias de atraso médio", situacao));
    }

    // --------------------------------------------------------------- modo 4

    /** O acumulado do período: o quanto a linha do recebido acompanha a do cobrado. */
    private Modo acumulado(List<Titulo> carteira, Periodo periodo) {
        Map<String, BigDecimal[]> meses = mesesDo(periodo);
        for (Titulo t : carteira) {
            if (t.getCompetencia() != null) {
                somar(meses, t.getCompetencia(), 0, t.getValor());
            }
            for (Pagamento p : t.getPagamentos()) {
                somar(meses, p.getPagoEm(), 1, p.getValor());
            }
        }

        List<Ponto> cobrado = new ArrayList<>();
        List<Ponto> recebido = new ArrayList<>();
        BigDecimal somaCobrado = BigDecimal.ZERO;
        BigDecimal somaRecebido = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal[]> mes : meses.entrySet()) {
            somaCobrado = somaCobrado.add(mes.getValue()[0]);
            somaRecebido = somaRecebido.add(mes.getValue()[1]);
            cobrado.add(new Ponto(mes.getKey(), somaCobrado));
            recebido.add(new Ponto(mes.getKey(), somaRecebido));
        }

        BigDecimal diferenca = somaCobrado.subtract(somaRecebido);
        String texto;
        String situacao;
        if (somaCobrado.signum() <= 0) {
            texto = "Sem cobrança no período, não há curva para comparar.";
            situacao = "neutra";
        } else if (porcento(diferenca, somaCobrado).compareTo(new BigDecimal("10")) <= 0) {
            texto = "As duas linhas andam quase juntas: o que é cobrado vira dinheiro "
                    + "no mesmo ritmo. É o desenho de uma carteira saudável.";
            situacao = "boa";
        } else {
            texto = "A distância entre as linhas é o dinheiro que foi cobrado e ficou "
                    + "pelo caminho. Quanto mais elas se afastam, mais a empresa está "
                    + "financiando o cliente sem cobrar por isso.";
            situacao = porcento(diferenca, somaCobrado)
                    .compareTo(new BigDecimal("25")) >= 0 ? "ruim" : "atencao";
        }

        return new Modo("acumulado", "Acumulado do período", "linhas-area",
                "A soma que vai crescendo mês a mês, do cobrado e do recebido.",
                List.of(new Serie("Cobrado acumulado", "linha", "tinta", cobrado),
                        new Serie("Recebido acumulado", "linha", "acento", recebido)),
                new Leitura("Distância entre cobrar e receber", texto,
                        "R$ " + dinheiro(diferenca) + " de diferença", situacao));
    }

    // --------------------------------------------------------------- modo 5

    /**
     * Curva ABC dos clientes: quem concentra o saldo em aberto.
     *
     * A régua de Pareto é a mesma usada em crédito: quando 20% dos clientes
     * respondem por 80% do saldo, a carteira é frágil a uma conversa só.
     */
    private Modo pareto(List<Titulo> carteira) {
        Map<String, BigDecimal> porCliente = new LinkedHashMap<>();
        for (Titulo t : carteira) {
            if (t.getSaldo().signum() <= 0) {
                continue;
            }
            porCliente.merge(t.getCliente().getRazaoSocial(), t.getSaldo(), BigDecimal::add);
        }

        List<Map.Entry<String, BigDecimal>> ordenados = new ArrayList<>(porCliente.entrySet());
        ordenados.sort(Map.Entry.<String, BigDecimal>comparingByValue(
                Comparator.reverseOrder()));
        BigDecimal total = ordenados.stream().map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Ponto> barras = new ArrayList<>();
        List<Ponto> acumulada = new ArrayList<>();
        BigDecimal caminhando = BigDecimal.ZERO;
        int limite = Math.min(ordenados.size(), 8);
        for (int i = 0; i < limite; i++) {
            Map.Entry<String, BigDecimal> linha = ordenados.get(i);
            caminhando = caminhando.add(linha.getValue());
            barras.add(new Ponto(curto(linha.getKey()), linha.getValue()));
            acumulada.add(new Ponto(curto(linha.getKey()), porcento(caminhando, total)));
        }

        BigDecimal doMaior = ordenados.isEmpty() ? BigDecimal.ZERO
                : porcento(ordenados.get(0).getValue(), total);
        String texto;
        String situacao;
        if (total.signum() <= 0) {
            texto = "Nada em aberto: não há concentração para medir.";
            situacao = "boa";
        } else if (doMaior.compareTo(new BigDecimal("40")) >= 0) {
            texto = "Um único cliente responde por mais de 40% do saldo. O resultado "
                    + "do mês depende de uma conversa só, e isso é risco, não relação "
                    + "comercial forte.";
            situacao = "ruim";
        } else if (ordenados.size() >= 3
                && porcento(ordenados.get(0).getValue().add(ordenados.get(1).getValue())
                .add(ordenados.get(2).getValue()), total)
                .compareTo(new BigDecimal("70")) >= 0) {
            texto = "Três clientes concentram mais de 70% do que está em aberto. "
                    + "A cobrança rende mais tratando esses três de perto do que "
                    + "mandando mensagem para todo mundo.";
            situacao = "atencao";
        } else {
            texto = "O saldo está espalhado entre vários clientes. Carteira assim "
                    + "sofre menos com um calote isolado, mas exige processo, porque "
                    + "não dá para cobrar um a um na mão.";
            situacao = "boa";
        }

        return new Modo("pareto", "Concentração por cliente", "barras-com-linha",
                "Os maiores saldos em aberto e o quanto eles somam, acumulado.",
                List.of(new Serie("Em aberto", "barra", "tinta", barras),
                        new Serie("Acumulado %", "linha", "acento", acumulada)),
                new Leitura("De quem o resultado depende", texto,
                        total.signum() <= 0 ? "sem saldo"
                                : "maior cliente: " + doMaior + "%", situacao));
    }

    // --------------------------------------------------------------- modo 6

    /**
     * Inadimplência por safra: de tudo que venceu naquele mês, quanto não foi
     * pago até hoje. É o indicador que mostra se o problema está piorando.
     */
    private Modo inadimplenciaPorSafra(List<Titulo> carteira, Periodo periodo) {
        Map<String, BigDecimal[]> meses = mesesDo(periodo);
        for (Titulo t : carteira) {
            if (t.getVencimento() == null) {
                continue;
            }
            String mes = t.getVencimento().format(MES_CURTO);
            if (!meses.containsKey(mes)) {
                continue;
            }
            meses.get(mes)[0] = meses.get(mes)[0].add(t.getValor());
            meses.get(mes)[1] = meses.get(mes)[1].add(t.getSaldo());
        }

        List<Ponto> indice = new ArrayList<>();
        List<Ponto> venceu = new ArrayList<>();
        meses.forEach((mes, valores) -> {
            indice.add(new Ponto(mes, porcento(valores[1], valores[0])));
            venceu.add(new Ponto(mes, valores[0]));
        });

        BigDecimal medio = mediaDe(indice);
        String texto;
        String situacao;
        if (somaDe(venceu).signum() <= 0) {
            texto = "Nada venceu no período escolhido.";
            situacao = "neutra";
        } else if (medio.compareTo(new BigDecimal("5")) <= 0) {
            texto = "A inadimplência das safras do período está abaixo de 5%. É o "
                    + "patamar em que a perda cabe no preço do serviço.";
            situacao = "boa";
        } else if (medio.compareTo(new BigDecimal("15")) <= 0) {
            texto = "Uma safra em cada sete reais não voltou no prazo. Ainda dá para "
                    + "recuperar boa parte, mas o custo da cobrança já pesa no mês.";
            situacao = "atencao";
        } else {
            texto = "Mais de 15% do que venceu no período segue em aberto. Nesse nível "
                    + "o problema não é de um cliente: é de política de crédito ou de "
                    + "cobrança, e precisa de decisão, não de insistência.";
            situacao = "ruim";
        }

        return new Modo("inadimplencia", "Inadimplência por safra", "barras-com-linha",
                "De tudo que venceu em cada mês, quanto ainda não foi pago.",
                List.of(new Serie("Venceu no mês", "barra", "tinta", venceu),
                        new Serie("Ainda em aberto %", "linha", "acento", indice)),
                new Leitura("Tendência da perda", texto, medio + "% em média", situacao));
    }

    // --------------------------------------------------------------- modo 7

    /** Previsão de entrada: o que vence adiante e o que a taxa histórica sugere. */
    private Modo previsao(List<Titulo> carteira) {
        YearMonth agora = YearMonth.now();
        Map<String, BigDecimal[]> meses = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            meses.put(agora.plusMonths(i).atDay(1).format(MES_CURTO),
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        LocalDate hoje = LocalDate.now();
        BigDecimal vencido = BigDecimal.ZERO;
        BigDecimal cobradoTotal = BigDecimal.ZERO;
        BigDecimal recebidoTotal = BigDecimal.ZERO;

        for (Titulo t : carteira) {
            cobradoTotal = cobradoTotal.add(t.getValor());
            recebidoTotal = recebidoTotal.add(t.getTotalPago());
            if (t.getSaldo().signum() <= 0 || t.getVencimento() == null) {
                continue;
            }
            if (t.getVencimento().isBefore(hoje)) {
                vencido = vencido.add(t.getSaldo());
                continue;
            }
            String mes = t.getVencimento().format(MES_CURTO);
            if (meses.containsKey(mes)) {
                meses.get(mes)[0] = meses.get(mes)[0].add(t.getSaldo());
            }
        }

        BigDecimal taxa = cobradoTotal.signum() == 0 ? BigDecimal.ZERO
                : recebidoTotal.divide(cobradoTotal, 4, RoundingMode.HALF_UP);

        List<Ponto> aVencer = new ArrayList<>();
        List<Ponto> estimado = new ArrayList<>();
        meses.forEach((mes, valores) -> {
            aVencer.add(new Ponto(mes, valores[0]));
            estimado.add(new Ponto(mes, valores[0].multiply(taxa)
                    .setScale(2, RoundingMode.HALF_UP)));
        });

        BigDecimal total = somaDe(aVencer);
        String texto;
        String situacao;
        if (total.signum() <= 0 && vencido.signum() <= 0) {
            texto = "Não há nada a vencer nem vencido: a carteira está zerada.";
            situacao = "neutra";
        } else if (vencido.compareTo(total) > 0) {
            texto = "O que já está vencido é maior do que tudo o que vai vencer nos "
                    + "próximos seis meses. O caixa futuro depende mais de recuperar "
                    + "o passado do que de faturar adiante.";
            situacao = "ruim";
        } else {
            texto = "A estimativa aplica a taxa de conversão da própria empresa sobre "
                    + "o que vence. É previsão, não promessa: serve para decidir "
                    + "compromisso, não para contar como recebido.";
            situacao = "atencao";
        }

        return new Modo("previsao", "Previsão de entrada", "barras-agrupadas",
                "O que vence nos próximos seis meses e o que a conversão histórica sugere.",
                List.of(new Serie("A vencer", "barra", "tinta", aVencer),
                        new Serie("Estimativa", "barra", "acento", estimado)),
                new Leitura("O que dá para contar", texto,
                        "R$ " + dinheiro(vencido) + " já vencido fora da conta", situacao));
    }

    // --------------------------------------------------------------- modo 8

    /** Como a carteira do período se divide entre pago, a vencer e vencido. */
    private Modo composicao(List<Titulo> carteira, Periodo periodo) {
        LocalDate hoje = LocalDate.now();
        BigDecimal pago = BigDecimal.ZERO;
        BigDecimal aVencer = BigDecimal.ZERO;
        BigDecimal vencido = BigDecimal.ZERO;

        for (Titulo t : carteira) {
            LocalDate referencia = t.getCompetencia() != null ? t.getCompetencia()
                    : t.getVencimento();
            if (referencia == null || referencia.isBefore(periodo.de())
                    || referencia.isAfter(periodo.ate())) {
                continue;
            }
            pago = pago.add(t.getTotalPago());
            if (t.getSaldo().signum() > 0) {
                if (t.getVencimento() != null && t.getVencimento().isBefore(hoje)) {
                    vencido = vencido.add(t.getSaldo());
                } else {
                    aVencer = aVencer.add(t.getSaldo());
                }
            }
        }

        List<Ponto> pontos = List.of(new Ponto("Recebido", pago),
                new Ponto("A vencer", aVencer), new Ponto("Vencido", vencido));
        BigDecimal total = somaDe(pontos);

        String texto;
        String situacao;
        if (total.signum() <= 0) {
            texto = "Nenhum título com competência neste período.";
            situacao = "neutra";
        } else if (porcento(vencido, total).compareTo(new BigDecimal("20")) >= 0) {
            texto = "Um quinto ou mais da carteira do período está vencido. Antes de "
                    + "vender mais, vale entender por que o que já foi vendido não foi pago.";
            situacao = "ruim";
        } else if (porcento(pago, total).compareTo(new BigDecimal("70")) >= 0) {
            texto = "A maior parte do período já virou dinheiro e o que sobrou ainda "
                    + "tem prazo. É a composição que permite planejar o mês seguinte.";
            situacao = "boa";
        } else {
            texto = "Boa parte do período ainda está para vencer. O número a acompanhar "
                    + "aqui não é o total, e sim quanto dessa fatia vira dinheiro no prazo.";
            situacao = "atencao";
        }

        return new Modo("composicao", "Composição da carteira", "rosca",
                "Como o período se divide entre recebido, a vencer e vencido.",
                List.of(new Serie("Carteira", "fatia", "tinta", pontos)),
                new Leitura("Retrato do período", texto,
                        total.signum() <= 0 ? "sem base"
                                : porcento(pago, total) + "% já recebido", situacao));
    }

    // ------------------------------------------------------------------ apoio

    private Map<String, BigDecimal[]> mesesDo(Periodo periodo) {
        Map<String, BigDecimal[]> meses = new LinkedHashMap<>();
        YearMonth inicio = YearMonth.from(periodo.de());
        YearMonth fim = YearMonth.from(periodo.ate());
        // Periodo muito longo vira os ultimos 24 meses: grafico com 300 colunas
        // nao e analise, e sim ruido.
        if (ChronoUnit.MONTHS.between(inicio, fim) > 24) {
            inicio = fim.minusMonths(23);
        }
        for (YearMonth mes = inicio; !mes.isAfter(fim); mes = mes.plusMonths(1)) {
            meses.put(mes.atDay(1).format(MES_CURTO),
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        return meses;
    }

    private void somar(Map<String, BigDecimal[]> meses, LocalDate dia, int coluna,
                       BigDecimal valor) {
        String mes = dia.format(MES_CURTO);
        BigDecimal[] valores = meses.get(mes);
        if (valores != null) {
            valores[coluna] = valores[coluna].add(valor);
        }
    }

    private BigDecimal soma(BigDecimal[] valores) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal valor : valores) {
            total = total.add(valor);
        }
        return total;
    }

    private BigDecimal somaDe(List<Ponto> pontos) {
        return pontos.stream().map(Ponto::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal mediaDe(List<Ponto> pontos) {
        List<Ponto> comValor = pontos.stream().filter(p -> p.valor().signum() > 0).toList();
        if (comValor.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return somaDe(comValor).divide(BigDecimal.valueOf(comValor.size()), 1,
                RoundingMode.HALF_UP);
    }

    private BigDecimal porcento(BigDecimal parte, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return parte.multiply(new BigDecimal("100")).divide(total, 1, RoundingMode.HALF_UP);
    }

    private String dinheiro(BigDecimal valor) {
        return String.format(java.util.Locale.of("pt", "BR"), "%,.2f",
                valor == null ? BigDecimal.ZERO : valor);
    }

    private String curto(String nome) {
        if (nome == null) {
            return "";
        }
        return nome.length() <= 16 ? nome : nome.substring(0, 15) + "…";
    }

    // ------------------------------------------------------------- respostas

    public record Periodo(String chave, String rotulo, LocalDate de, LocalDate ate) {
    }

    public record Ponto(String rotulo, BigDecimal valor) {
    }

    public record Serie(String nome, String desenho, String cor, List<Ponto> pontos) {
    }

    /** A leitura do resultado: o que aquele desenho quer dizer para esta empresa. */
    public record Leitura(String titulo, String texto, String indicador, String situacao) {
    }

    public record Modo(String chave, String nome, String tipo, String descricao,
                       List<Serie> series, Leitura leitura) {
    }

    public record Painel(String titulo, String pergunta, List<Modo> modos) {
    }

    public record Resposta(Periodo periodo, List<Periodo> periodos, Painel esquerda,
                           Painel direita) {
    }
}
