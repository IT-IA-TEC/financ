package br.com.itia.financeiro;

/**
 * A porta de entrada do programa.
 *
 * Existe separada da janela por uma exigência prática do Java: quando o sistema
 * é empacotado como programa instalável, a classe que abre o processo não pode
 * ser a própria janela.
 */
public final class Inicio {

    private Inicio() {
    }

    public static void main(String[] args) {
        FinanceiroApplication.main(args);
    }
}
