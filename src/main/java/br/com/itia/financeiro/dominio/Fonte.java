package br.com.itia.financeiro.dominio;

/**
 * De onde veio o dado.
 *
 * Todo campo que pode chegar de fora carrega a fonte. É isso que permite saber
 * se alguém digitou aqui, se veio da Receita, da plataforma de cobrança ou do
 * banco, e evita sobrescrever informação boa com informação velha.
 */
public enum Fonte {

    MANUAL("Digitado aqui"),
    CNPJA("Receita, pela CNPJá"),
    CONEXA("Plataforma de cobrança"),
    BANCO("Banco"),
    ERP("ERP do grupo"),
    IMPORTACAO("Importação de arquivo");

    private final String rotulo;

    Fonte(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
