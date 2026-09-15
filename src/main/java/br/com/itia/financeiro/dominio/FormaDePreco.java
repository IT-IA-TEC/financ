package br.com.itia.financeiro.dominio;

/** Como o preço de um serviço é calculado. */
public enum FormaDePreco {

    VALOR_UNICO("Valor único",
            "Um preço pelo serviço completo, incluindo os itens que fazem parte dele."),

    SOMA_DOS_ITENS("Soma dos itens",
            "O total sai da soma dos itens realizados. O serviço em si não tem valor próprio."),

    BASE_MAIS_ADICIONAIS("Valor-base mais adicionais",
            "Um preço pelo serviço, acrescido dos itens que têm cobrança própria.");

    private final String rotulo;
    private final String comoFunciona;

    FormaDePreco(String rotulo, String comoFunciona) {
        this.rotulo = rotulo;
        this.comoFunciona = comoFunciona;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getComoFunciona() {
        return comoFunciona;
    }

    /** Se o serviço tem valor próprio nesta forma. */
    public boolean temValorDoServico() {
        return this != SOMA_DOS_ITENS;
    }
}
