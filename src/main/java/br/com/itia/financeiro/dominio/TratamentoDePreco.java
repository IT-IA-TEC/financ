package br.com.itia.financeiro.dominio;

/** Se o item já está dentro do preço do serviço ou é cobrado à parte. */
public enum TratamentoDePreco {

    INCLUIDO("Incluído no serviço",
            "Faz parte do preço do serviço e não é somado de novo."),

    COBRANCA_PROPRIA("Cobrança própria",
            "Tem valor próprio e entra no total quando for realizado.");

    private final String rotulo;
    private final String comoFunciona;

    TratamentoDePreco(String rotulo, String comoFunciona) {
        this.rotulo = rotulo;
        this.comoFunciona = comoFunciona;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getComoFunciona() {
        return comoFunciona;
    }

    public boolean cobraSeparado() {
        return this == COBRANCA_PROPRIA;
    }
}
