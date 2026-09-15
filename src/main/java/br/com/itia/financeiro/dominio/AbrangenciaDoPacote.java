package br.com.itia.financeiro.dominio;

/** Se o pacote inclui o serviço inteiro ou só um item dele. */
public enum AbrangenciaDoPacote {

    SERVICO_COMPLETO("Serviço completo",
            "Tudo que faz parte do serviço entra no pacote."),

    ITEM_ESCOLHIDO("Somente um item",
            "Só o item escolhido entra. Para incluir mais de um, repita a linha.");

    private final String rotulo;
    private final String comoFunciona;

    AbrangenciaDoPacote(String rotulo, String comoFunciona) {
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
