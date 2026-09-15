package br.com.itia.financeiro.dominio;

/** Em que pé está o registro do atendimento. */
public enum SituacaoDoAtendimento {

    REGISTRADO("registrado"),
    CANCELADO("cancelado");

    private final String rotulo;

    SituacaoDoAtendimento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
