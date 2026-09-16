package br.com.itia.financeiro.tela;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * A janela que abre por cima da tela, no padrão da marca.
 *
 * É a mesma janela de sempre: topo branco com o título e o fio preto embaixo,
 * corpo que rola quando o conteúdo é grande, e o pé com Cancelar de um lado e a
 * ação de verdade do outro. Enquanto ela está aberta, o resto do sistema espera.
 *
 * Existe para que nenhuma ação precise da caixinha cinza do Java, que não tem
 * nada a ver com a identidade.
 */
public final class JanelaFlutuante {

    /**
     * A janela ocupa sempre 85% da largura e 85% da altura, aconteça o que
     * acontecer com o conteúdo: trocar de aba não muda o tamanho, e quem rola
     * é o miolo. A estreita usa 55%, para conteúdo curto.
     */
    private static final double PARTE = 0.85;
    private static final double PARTE_ESTREITA = 0.55;

    private final Stage palco = new Stage();
    private final VBox corpo = new VBox(14);
    private final HBox pe = new HBox(12);
    private final Label titulo = new Label();
    private final Label apoio = new Label();

    private JanelaFlutuante(Stage dono, String nome, String explicacao, boolean estreita) {
        titulo.setText(nome);
        titulo.getStyleClass().add("titulo-janela");

        apoio.setText(explicacao == null ? "" : explicacao);
        apoio.getStyleClass().add("apoio-janela");
        apoio.setWrapText(true);
        apoio.setVisible(explicacao != null && !explicacao.isBlank());
        apoio.setManaged(apoio.isVisible());

        Button fechar = new Button("✕");
        fechar.getStyleClass().add("fechar-janela");
        fechar.setOnAction(clique -> palco.close());

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);

        HBox topo = new HBox(16, new VBox(2, titulo, apoio), espaco, fechar);
        topo.getStyleClass().add("topo-janela");
        topo.setAlignment(Pos.TOP_LEFT);

        corpo.setPadding(new Insets(22));
        ScrollPane rolagem = new ScrollPane(corpo);
        rolagem.setFitToWidth(true);
        rolagem.getStyleClass().add("corpo-janela");

        pe.getStyleClass().add("pe-janela");
        pe.setAlignment(Pos.CENTER_RIGHT);
        pe.setPadding(new Insets(16, 22, 16, 22));

        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("janela-flutuante");
        raiz.setTop(topo);
        raiz.setCenter(rolagem);
        raiz.setBottom(pe);

        double parte = estreita ? PARTE_ESTREITA : PARTE;
        double largura = dono == null ? 900 : dono.getWidth() * parte;
        double altura = dono == null ? 700 : dono.getHeight() * PARTE;
        Scene cena = new Scene(raiz, largura, altura);
        cena.getStylesheets().addAll(dono == null ? java.util.List.of()
                : dono.getScene().getStylesheets());

        palco.initStyle(StageStyle.UTILITY);
        palco.initModality(Modality.APPLICATION_MODAL);
        if (dono != null) {
            palco.initOwner(dono);
        }
        palco.setTitle(nome);
        palco.setScene(cena);
        palco.setResizable(false);
    }

    /** Abre a janela larga, do tamanho de sempre. */
    public static JanelaFlutuante nova(Stage dono, String nome, String explicacao) {
        return new JanelaFlutuante(dono, nome, explicacao, false);
    }

    /** Abre a janela estreita, para quando o conteúdo é pouco. */
    public static JanelaFlutuante estreita(Stage dono, String nome, String explicacao) {
        return new JanelaFlutuante(dono, nome, explicacao, true);
    }

    /** Põe conteúdo no corpo da janela. */
    public JanelaFlutuante com(Node... partes) {
        corpo.getChildren().addAll(partes);
        return this;
    }

    /**
     * O botão principal do pé. Se a ação devolver verdadeiro, a janela fecha;
     * se devolver falso, ela continua aberta com o que a pessoa já escreveu.
     */
    public JanelaFlutuante acao(String texto, java.util.function.BooleanSupplier oQueFazer) {
        Button botao = Pecas.botao(texto, () -> {
            if (oQueFazer.getAsBoolean()) {
                palco.close();
            }
        });
        pe.getChildren().add(botao);
        return this;
    }

    /** Um botão a mais no pé, sem fechar a janela. */
    public JanelaFlutuante outraAcao(String texto, Runnable oQueFazer) {
        pe.getChildren().add(Pecas.botaoVazado(texto, oQueFazer));
        return this;
    }

    /** Abre e espera a pessoa resolver. */
    public void abrir() {
        if (pe.getChildren().isEmpty()) {
            pe.getChildren().add(Pecas.botaoVazado("Fechar", palco::close));
        } else {
            pe.getChildren().add(0, Pecas.botaoVazado("Cancelar", palco::close));
        }
        palco.showAndWait();
    }

    /** Fecha a janela de fora, quando a própria ação já resolveu. */
    public void fechar() {
        palco.close();
    }
}
