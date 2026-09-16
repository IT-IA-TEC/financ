package br.com.itia.financeiro.tela;

import java.util.UUID;

/**
 * Uma tela que mostra um registro só: um título, um cliente, uma conta.
 *
 * A janela avisa qual é antes de mandar montar.
 */
public interface TelaDeUmSo extends Tela {

    void escolher(UUID id);
}
