package br.com.itia.financeiro.dominio;

/** De quanto em quanto tempo o pacote é cobrado. */
public enum Periodicidade {

    MENSAL("Mensal", "Cobrado todo mês."),
    TRIMESTRAL("Trimestral", "Cobrado a cada três meses."),
    SEMESTRAL("Semestral", "Cobrado a cada seis meses."),
    ANUAL("Anual", "Cobrado uma vez por ano."),
    AVULSA("Avulsa", "Cobrado uma vez só, sem repetição."),
    OUTRA("Outra", "Um intervalo próprio, escrito ao lado.");

    private final String rotulo;
    private final String comoFunciona;

    Periodicidade(String rotulo, String comoFunciona) {
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
