package br.com.itia.financeiro.dominio;

/**
 * O que ja foi feito no banco.
 *
 * Encaminhar uma solicitacao nao comprova pagamento, e agendar no banco
 * tambem nao. Por isso execucao e liquidacao sao coisas diferentes.
 */
public enum Execucao {

    NAO_ENCAMINHADA("não encaminhada",
            "Ninguém mandou pagar ainda."),

    ENCAMINHADA("encaminhada",
            "Foi pedido a quem paga, sem confirmação."),

    AGENDADA("agendada",
            "Marcada no banco para uma data."),

    FALHOU("falhou",
            "A tentativa não completou.");

    private final String rotulo;
    private final String comoFunciona;

    Execucao(String rotulo, String comoFunciona) {
        this.rotulo = rotulo;
        this.comoFunciona = comoFunciona;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getComoFunciona() {
        return comoFunciona;
    }
}
