package br.com.itia.financeiro.dominio;

/** De onde a cobranca nasceu. */
public enum OrigemDaCobranca {

    PACOTE("Pacote", "Mensalidade ou outra periodicidade da contratacao do cliente."),
    SERVICO("Serviço", "Serviço avulso, item adicional ou excedente do pacote.");

    private final String rotulo;
    private final String comoFunciona;

    OrigemDaCobranca(String rotulo, String comoFunciona) {
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
