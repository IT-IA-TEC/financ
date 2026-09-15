package br.com.itia.financeiro.dominio;

/**
 * O grupo do plano gerencial.
 *
 * Existe para o relatorio nao somar toda saida do banco como custo: compra de
 * bem, emprestimo e troco entre contas proprias nao sao despesa.
 */
public enum GrupoDeNatureza {

    DESPESA("Despesa",
            "Consumo do período. Entra no custo."),

    AQUISICAO("Aquisição patrimonial",
            "Compra de bem. Vira patrimônio, e não custo do mês."),

    FINANCIAMENTO("Financiamento",
            "Principal e encargos, separados um do outro."),

    TRANSFERENCIA("Transferência",
            "Dinheiro indo de uma conta própria para outra."),

    RECEITA("Receita",
            "Entrada de dinheiro pelo serviço prestado."),

    SOCIOS("Movimentação com sócios",
            "Pró-labore, lucros, aportes e devolucoes."),

    TRIBUTO("Tributo",
            "Imposto e contribuição.");

    private final String rotulo;
    private final String comoFunciona;

    GrupoDeNatureza(String rotulo, String comoFunciona) {
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
