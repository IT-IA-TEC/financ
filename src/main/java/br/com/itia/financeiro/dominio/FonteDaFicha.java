package br.com.itia.financeiro.dominio;

import java.util.Arrays;
import java.util.List;

/**
 * De onde sai cada linha da ficha do caso.
 *
 * A ficha é a resposta para uma pergunta só: "o que eu preciso saber antes de
 * falar de dinheiro com este cliente". Por isso tudo aqui é número de
 * cobrança, e não assunto de atendimento: quanto deve, há quanto tempo, como
 * costuma pagar, o que já foi combinado.
 *
 * Cada empresa liga as linhas que quer e na ordem que quer, porque uma
 * contabilidade que cobra mensalidade não olha as mesmas coisas que uma
 * empresa que vende por pedido.
 */
public enum FonteDaFicha {

    EM_ABERTO("Em aberto", "Tudo que este cliente deve hoje, e em quantos documentos."),

    VENCIDO("Vencido", "Quanto do que ele deve já passou do vencimento."),

    MAIOR_ATRASO("Atraso", "Quantos dias tem o documento mais antigo em aberto."),

    DOCUMENTO_MAIS_ANTIGO("Documento mais antigo",
            "Qual é o documento que está parado há mais tempo."),

    PROXIMO_VENCIMENTO("Próximo a vencer", "O que vence em seguida, e quando."),

    COMO_PAGA("Como costuma pagar",
            "A média de atraso dos pagamentos que ele já fez aqui."),

    JA_PAGOU("Já pagou", "Quanto este cliente já pagou, no total."),

    ATRASO_MEDIO("Atraso médio", "Em média, quantos dias ele demora para pagar."),

    COMPROVANTES("Comprovantes", "Quantas provas de pagamento ele já mandou."),

    PROMESSAS_CUMPRIDAS("Promessas cumpridas",
            "Quantas promessas ele cumpriu, de quantas que fez."),

    UNIDADES("Unidades do mesmo dono",
            "As outras empresas do mesmo pagador, com o saldo de cada uma."),

    ULTIMA_COBRANCA("Última cobrança", "Quando falamos de dinheiro com ele pela última vez."),

    ULTIMA_FALA("Última fala do cliente", "A última coisa que ele escreveu."),

    PROMESSA("Promessa", "Se ele prometeu pagar e para quando."),

    ACORDO("Acordo", "Se tem acordo em andamento, cumprido ou quebrado."),

    SITUACAO_DO_CASO("Situação", "Em cobrança, em acordo, contestado, parado ou jurídico."),

    RESPONSAVEL("Quem cuida", "Quem da equipe está com este caso."),

    PROXIMA_ACAO("Combinado", "O que ficou de fazer, e em que data."),

    TOM_DE_COBRANCA("Tom combinado", "O jeito de falar que ficou acertado com este cliente."),

    ACEITA_PARCELAMENTO("Parcelamento", "Se este cliente aceita dividir a dívida."),

    TEMPO_DE_CASA("Tempo de casa", "Desde quando ele é cliente."),

    RESUMO_DA_IA("Leitura do modelo",
            "Um resumo do caso escrito pelo modelo de inteligência, quando ligado.");

    private final String rotuloPadrao;
    private final String explicacao;

    FonteDaFicha(String rotuloPadrao, String explicacao) {
        this.rotuloPadrao = rotuloPadrao;
        this.explicacao = explicacao;
    }

    public String getRotuloPadrao() {
        return rotuloPadrao;
    }

    public String getExplicacao() {
        return explicacao;
    }

    /** As linhas que uma empresa recebe antes de configurar qualquer coisa. */
    public static List<FonteDaFicha> daFabrica() {
        return List.of(EM_ABERTO, DOCUMENTO_MAIS_ANTIGO, JA_PAGOU, ATRASO_MEDIO,
                COMPROVANTES, PROMESSAS_CUMPRIDAS, ACORDO, PROXIMA_ACAO);
    }

    public static List<FonteDaFicha> todas() {
        return Arrays.asList(values());
    }
}
