package br.com.itia.financeiro.dominio;

/** A aprovacao anda separada do pagamento. */
public enum Aprovacao {

    NAO_EXIGIDA("não exigida",
            "Pela regra da empresa, este gasto não precisa de aprovação."),

    PENDENTE("pendente",
            "Esperando alguém aprovar."),

    APROVADA("aprovada",
            "Liberada para entrar na fila de pagamento."),

    REJEITADA("rejeitada",
            "Recusada, com motivo registrado.");

    private final String rotulo;
    private final String comoFunciona;

    Aprovacao(String rotulo, String comoFunciona) {
        this.rotulo = rotulo;
        this.comoFunciona = comoFunciona;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getComoFunciona() {
        return comoFunciona;
    }

    public boolean liberaPagamento() {
        return this == NAO_EXIGIDA || this == APROVADA;
    }
}
