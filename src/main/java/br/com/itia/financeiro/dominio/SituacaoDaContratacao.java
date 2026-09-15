package br.com.itia.financeiro.dominio;

/** Em que pé está a contratação de um pacote por um cliente. */
public enum SituacaoDaContratacao {

    ATIVA("ativa"),
    SUSPENSA("suspensa"),
    ENCERRADA("encerrada");

    private final String rotulo;

    SituacaoDaContratacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
