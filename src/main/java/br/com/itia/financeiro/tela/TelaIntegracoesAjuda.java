package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.dominio.Conector;
import br.com.itia.financeiro.dominio.TipoAutenticacao;
import br.com.itia.financeiro.dominio.TipoIntegracao;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * A tela de ajuda das integrações.
 *
 * Explica as duas decisões de toda ligação: como ela funciona e como o outro
 * lado sabe que somos nós. Traz também os modelos prontos e o que o sistema
 * garante em qualquer integração.
 */
@Component
public class TelaIntegracoesAjuda implements Tela {

    private final Janela janela;

    public TelaIntegracoesAjuda(@Lazy Janela janela) {
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "integracoes";
    }

    @Override
    public Node montar() {
        VBox tela = new VBox(16);
        tela.getChildren().add(Pecas.cabecalho("documentação", "O que dá para integrar",
                "Toda ligação tem duas decisões: como ela funciona e como o outro lado sabe "
                        + "que somos nós.",
                Pecas.botaoVazado("Voltar", () -> janela.ir(TelaIntegracoes.class))));

        tela.getChildren().add(Pecas.secao("Como a ligação funciona"));
        tela.getChildren().add(Tabela.de(List.of(TipoIntegracao.values()))
                .coluna("Forma", TipoIntegracao::getRotulo, 1.2)
                .coluna("Para que serve", TipoIntegracao::getResumo, 2.4)
                .coluna("O que dá para fazer", t -> String.join(" · ", t.getOQueDaParaFazer()), 2.4)
                .montar());

        tela.getChildren().add(Pecas.secao("Como o outro lado sabe que somos nós"));
        tela.getChildren().add(Tabela.de(List.of(TipoAutenticacao.values()))
                .coluna("Forma de acesso", TipoAutenticacao::getRotulo, 1.4)
                .coluna("Quando usar", TipoAutenticacao::getResumo, 2.6)
                .coluna("O que você precisa ter em mãos", a -> a.getCampos().isEmpty()
                        ? "nada" : a.getCampos().stream()
                                .map(TipoAutenticacao.Campo::rotulo)
                                .collect(java.util.stream.Collectors.joining(", ")), 2)
                .montar());

        tela.getChildren().add(Pecas.secao("Modelos prontos"));
        Label explicacao = new Label("Cada modelo é um atalho: preenche tipo, acesso e "
                + "operações de uma vez. Os caminhos das operações são ponto de partida, "
                + "e devem ser conferidos na documentação do provedor.");
        explicacao.getStyleClass().add("dica");
        explicacao.setWrapText(true);
        tela.getChildren().add(explicacao);

        tela.getChildren().add(Tabela.de(List.of(Conector.values()))
                .coluna("Modelo", Conector::getRotulo, 1.4)
                .coluna("Para que serve", Conector::getResumo, 2.4)
                .coluna("Tipo", c -> c.getTipoSugerido() == null ? ""
                        : c.getTipoSugerido().getRotulo(), 1.2)
                .coluna("Acesso", c -> c.getAutenticacaoSugerida() == null ? ""
                        : c.getAutenticacaoSugerida().getRotulo(), 1.4)
                .coluna("Operações que já vêm", c -> c.getOperacoesSugeridas().isEmpty()
                        ? "nenhuma, você cadastra"
                        : c.getOperacoesSugeridas().size() + " operações", 1.4)
                .montar());

        tela.getChildren().add(Pecas.secao("O que o sistema garante, em qualquer integração"));
        tela.getChildren().add(Pecas.caixa(
                garantia("Credencial cifrada",
                        "Guardada cifrada e nunca devolvida para a tela. Segredo salvo aparece "
                                + "só com os últimos dígitos."),
                garantia("Nada roda sem estar cadastrado",
                        "Só existe chamada para fora se a operação estiver cadastrada, com "
                                + "verbo e caminho."),
                garantia("Tudo fica registrado",
                        "Cada entrada e cada saída vira registro com data, situação e conteúdo, "
                                + "guardado bruto."),
                garantia("Falha passageira tenta de novo",
                        "Tempo limite, número de tentativas e espera entre elas são "
                                + "configurados por integração."),
                garantia("Endereço de recebimento secreto",
                        "Cada integração tem o seu, com trecho secreto trocável. Desligada, a "
                                + "porta não aceita nada."),
                garantia("Separação por empresa",
                        "Integração pertence a uma empresa. Uma nunca enxerga nem recebe pela "
                                + "outra.")));
        return tela;
    }

    private VBox garantia(String titulo, String explicacao) {
        Label nome = new Label(titulo);
        nome.getStyleClass().add("rotulo-campo");

        Label texto = new Label(explicacao);
        texto.getStyleClass().add("dica");
        texto.setWrapText(true);

        return new VBox(2, nome, texto);
    }
}
