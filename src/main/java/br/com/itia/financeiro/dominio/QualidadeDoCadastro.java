package br.com.itia.financeiro.dominio;

/** O quanto o registro esta completo. Rascunho nao se perde nem engana. */
public enum QualidadeDoCadastro {

    RASCUNHO("rascunho",
            "Anotado e ainda não conferido."),

    INCOMPLETO("incompleto",
            "Falta informação para pagar com segurança."),

    COMPLETO("completo",
            "Tem tudo o que precisa.");

    private final String rotulo;
    private final String comoFunciona;

    QualidadeDoCadastro(String rotulo, String comoFunciona) {
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
