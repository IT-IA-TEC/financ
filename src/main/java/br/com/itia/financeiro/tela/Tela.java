package br.com.itia.financeiro.tela;

import javafx.scene.Node;

/**
 * Uma tela do sistema.
 *
 * Cada tela sabe duas coisas: qual item do menu acende quando ela está aberta,
 * e como se montar. Ela é montada de novo a cada vez que é aberta, então nunca
 * mostra número velho.
 */
public interface Tela {

    /** O item do menu que acende. Vazio quando a tela não fica no menu. */
    String secao();

    /** O conteúdo da tela, já pronto para aparecer. */
    Node montar();

    /**
     * Telas que ocupam a janela inteira, sem a faixa do topo e sem folga nas
     * bordas. É o caso da porta de entrada: ali ainda não existe menu.
     */
    /**
     * Telas de mesa cabem inteiras na janela: o topo e os números ficam parados
     * e cada coluna rola por dentro. Quem diz sim aqui não rola a página.
     */
    default boolean cabeNaTela() {
        return false;
    }

    default boolean ocupaTudo() {
        return false;
    }
}
