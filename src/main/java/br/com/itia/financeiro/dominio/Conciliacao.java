package br.com.itia.financeiro.dominio;

/** Se o pagamento ja foi conferido contra o extrato do banco. */
public enum Conciliacao {

    NAO_CONCILIADA("não conciliada",
            "Ainda não conferida com o extrato."),

    PARCIAL("parcial",
            "Parte dos pagamentos bateu com o extrato."),

    CONCILIADA("conciliada",
            "Confere com o extrato do banco.");

    private final String rotulo;
    private final String comoFunciona;

    Conciliacao(String rotulo, String comoFunciona) {
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
