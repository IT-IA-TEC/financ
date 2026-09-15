package br.com.itia.financeiro.dominio;

/** Em que janela de tempo a quantidade incluída vale. */
public enum PeriodoDoLimite {

    POR_PERIODO_DO_PACOTE("A cada período do pacote",
            "Segue a periodicidade do pacote. Pacote mensal, limite por mês."),

    POR_MES("Por mês", "Sempre por mês, mesmo que o pacote seja cobrado em outro intervalo."),
    POR_ANO("Por ano", "A conta vira uma vez por ano."),
    TOTAL_DA_VIGENCIA("No total da vigência", "Vale para o contrato inteiro, sem renovar.");

    private final String rotulo;
    private final String comoFunciona;

    PeriodoDoLimite(String rotulo, String comoFunciona) {
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
