package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.servico.Esteira;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Inadimplência: quem está devendo, num lugar só.
 *
 * A aba ainda está em branco de propósito. O aviso vermelho da coluna lateral
 * já mostra quantos clientes têm título vencido e traz para cá.
 */
@Component
public class TelaInadimplencia implements Tela {

    private final Esteira esteira;
    private final Janela janela;

    public TelaInadimplencia(Esteira esteira, @Lazy Janela janela) {
        this.esteira = esteira;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "cobranca";
    }

    @Override
    public Node montar() {
        int quantos = esteira.quantosInadimplentes(LocalDate.now());

        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("cobrança", "Inadimplência",
                "Os clientes com título vencido.",
                Pecas.quadrosDoTopo(
                        Pecas.quadro("Clientes inadimplentes", String.valueOf(quantos),
                                "com pelo menos um título vencido", true, false))));
        tela.getChildren().add(AbasDaCobranca.montar(janela, "Inadimplência"));
        tela.getChildren().add(Pecas.vazio("A lista dos clientes entra aqui."));
        return tela;
    }
}
