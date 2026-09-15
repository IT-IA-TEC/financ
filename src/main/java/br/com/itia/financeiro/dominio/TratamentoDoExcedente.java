package br.com.itia.financeiro.dominio;

/** O que acontece quando o cliente passa da quantidade incluída. */
public enum TratamentoDoExcedente {

    PRECO_DO_CATALOGO("Cobrar pelo preço cadastrado",
            "O que passar do limite é cobrado pelo preço do catálogo."),

    EXIGE_AVALIACAO("Exigir avaliação",
            "O que passar do limite para e espera alguém decidir o preço.");

    private final String rotulo;
    private final String comoFunciona;

    TratamentoDoExcedente(String rotulo, String comoFunciona) {
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
