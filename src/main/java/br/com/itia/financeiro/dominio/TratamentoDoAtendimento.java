package br.com.itia.financeiro.dominio;

/** Como o atendimento vai ser tratado na hora de cobrar. */
public enum TratamentoDoAtendimento {

    INCLUIDO_NO_PACOTE("Incluído no pacote",
            "Já está pago pelo pacote do cliente. Conta contra o limite e não gera cobrança."),

    COBRADO_A_PARTE("Cobrado à parte",
            "Não está no pacote, ou passou do limite. Gera cobrança pelo preço do catálogo."),

    COM_DESCONTO("Cobrado com desconto",
            "Gera cobrança com um abatimento só deste atendimento. O catálogo não muda."),

    GRATUITO("Gratuito",
            "Não gera cobrança e não consome o limite do pacote. O motivo fica na observação.");

    private final String rotulo;
    private final String comoFunciona;

    TratamentoDoAtendimento(String rotulo, String comoFunciona) {
        this.rotulo = rotulo;
        this.comoFunciona = comoFunciona;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getComoFunciona() {
        return comoFunciona;
    }

    /** Se este tratamento consome a quantidade incluída no pacote. */
    public boolean consomeLimite() {
        return this == INCLUIDO_NO_PACOTE;
    }

    public boolean geraCobranca() {
        return this == COBRADO_A_PARTE || this == COM_DESCONTO;
    }
}
