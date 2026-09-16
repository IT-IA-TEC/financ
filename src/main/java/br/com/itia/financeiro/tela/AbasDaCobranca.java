package br.com.itia.financeiro.tela;

import javafx.scene.Node;

import java.util.LinkedHashMap;

/**
 * As oito partes do módulo Cobrança, sempre na mesma ordem.
 *
 * Fica separado para nenhuma tela do módulo inventar uma ordem própria.
 */
final class AbasDaCobranca {

    private AbasDaCobranca() {
    }

    static Node montar(Janela janela, String atual) {
        LinkedHashMap<String, Runnable> partes = new LinkedHashMap<>();
        partes.put("Disparos", () -> janela.ir(TelaDisparos.class));
        partes.put("Conversas", () -> janela.ir(TelaConversas.class));
        partes.put("Régua", () -> janela.ir(TelaRegua.class));
        partes.put("Esteira", () -> janela.ir(TelaEsteira.class));
        partes.put("Acordos", () -> janela.ir(TelaAcordos.class));
        partes.put("Fluxos", () -> janela.ir(TelaFluxos.class));
        partes.put("Agente", () -> janela.ir(TelaAgente.class));
        partes.put("Modelos", () -> janela.ir(TelaModelos.class));
        return Pecas.abas(atual, partes);
    }
}
