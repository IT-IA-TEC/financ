package br.com.itia.financeiro.dominio;

/** Em que pe esta a cobranca. */
public enum SituacaoDaCobranca {

    PENDENTE("pendente", "Aguardando conferencia e aprovacao."),
    APROVADA("aprovada", "Aprovada e ja virou titulo no financeiro."),
    ENVIADA("enviada", "Enviada para o sistema de cobranca."),
    PAGA("paga", "O titulo correspondente foi quitado."),
    CANCELADA("cancelada", "Nao vale mais."),
    SEM_VALOR("sem valor a receber", "Gratuidade registrada, sem valor a receber.");

    private final String rotulo;
    private final String comoFunciona;

    SituacaoDaCobranca(String rotulo, String comoFunciona) {
        this.rotulo = rotulo;
        this.comoFunciona = comoFunciona;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getComoFunciona() {
        return comoFunciona;
    }

    /** Se ainda da para mexer no valor desta cobranca. */
    public boolean aindaEditavel() {
        return this == PENDENTE;
    }
}
