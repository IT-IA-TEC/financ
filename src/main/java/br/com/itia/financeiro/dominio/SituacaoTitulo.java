package br.com.itia.financeiro.dominio;

/** Em que pe esta o titulo. Quem manda nisso e o saldo, nunca a digitacao. */
public enum SituacaoTitulo {
    ABERTO,
    PARCIAL,
    PAGO,
    /**
     * Substituido por um acordo.
     *
     * O documento nao some e nao vira pagamento que nunca entrou: ele sai da
     * cobranca porque quem passa a ser cobrado sao as parcelas do acordo. Se o
     * acordo quebrar, ele volta a ser ABERTO ou PARCIAL.
     */
    EM_ACORDO,
    CANCELADO
}
