package br.com.itia.financeiro.dominio;

/** De onde veio a informacao. */
public enum OrigemDoRegistro {

    MANUAL("Digitado",
            "Alguém cadastrou na tela."),

    WHATSAPP("WhatsApp",
            "Veio de uma mensagem, e a conversa fica ligada ao registro."),

    IMPORTACAO("Importação",
            "Veio de um arquivo."),

    RECORRENCIA("Recorrência",
            "Gerado pelo molde de conta que se repete."),

    INTEGRACAO("Integração",
            "Veio de outro sistema por API.");

    private final String rotulo;
    private final String comoFunciona;

    OrigemDoRegistro(String rotulo, String comoFunciona) {
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
