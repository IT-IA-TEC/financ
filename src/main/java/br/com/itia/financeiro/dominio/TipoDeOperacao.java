package br.com.itia.financeiro.dominio;

/**
 * Como a obrigacao surgiu.
 *
 * Nao substitui a natureza do gasto: o reembolso de uma fonte de computador
 * continua sendo manutencao de equipamento na composicao.
 */
public enum TipoDeOperacao {

    DESPESA("Despesa comum",
            "Conta do dia a dia."),

    REEMBOLSO("Reembolso",
            "Alguém pagou do próprio bolso e vai receber de volta."),

    FOLHA("Folha e benefícios",
            "Salário, VT, VA e encargos, por competência."),

    MANUTENCAO("Manutenção de bem",
            "Conserto ou peça de um equipamento."),

    AQUISICAO("Aquisição de bem",
            "Compra que vira patrimônio."),

    TRIBUTO("Tributo",
            "Imposto, contribuição e taxa."),

    PRO_LABORE("Pró-labore",
            "Remuneração do sócio pelo trabalho."),

    DISTRIBUICAO("Distribuição de lucros",
            "Retirada de lucro do sócio."),

    APORTE("Aporte de sócio",
            "Dinheiro do sócio entrando na empresa."),

    TRANSFERENCIA("Transferência entre contas",
            "Movimento entre contas próprias."),

    FINANCIAMENTO("Parcela de financiamento",
            "Principal e encargos do empréstimo.");

    private final String rotulo;
    private final String comoFunciona;

    TipoDeOperacao(String rotulo, String comoFunciona) {
        this.rotulo = rotulo;
        this.comoFunciona = comoFunciona;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getComoFunciona() {
        return comoFunciona;
    }

    /** Se este tipo pede o nome de quem pagou antes e o motivo. */
    public boolean pedeQuemPagouAntes() {
        return this == REEMBOLSO;
    }

    public boolean pedeBem() {
        return this == MANUTENCAO || this == AQUISICAO;
    }

    public boolean pedePessoa() {
        return this == FOLHA || this == PRO_LABORE || this == DISTRIBUICAO || this == APORTE;
    }
}
