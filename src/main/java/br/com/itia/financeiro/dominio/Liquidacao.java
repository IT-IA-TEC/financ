package br.com.itia.financeiro.dominio;

/** Quanto ja foi pago da obrigacao. Sai da soma dos pagamentos. */
public enum Liquidacao {

    EM_ABERTO("em aberto",
            "Nada pago ainda."),

    PARCIAL("parcial",
            "Pago em parte, com saldo."),

    LIQUIDADA("liquidada",
            "Não sobra saldo.");

    private final String rotulo;
    private final String comoFunciona;

    Liquidacao(String rotulo, String comoFunciona) {
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
