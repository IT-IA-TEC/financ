package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.marca.ItiaFonts;
import br.com.itia.financeiro.marca.ItiaTokens;
import br.com.itia.financeiro.marca.TrackedText;
import br.com.itia.financeiro.servico.Acessos;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * A porta de entrada: um card só, no meio da tela.
 *
 * As medidas saíram do desenho: card de 420 de largura, canto 18, fio laranja
 * de 1, folga de 44 por 40, marca de 44 em cima, título de 28 e o botão de 48
 * ocupando a linha inteira.
 *
 * Ainda sem senha e sem código de confirmação: a conferência de acesso entra
 * na etapa combinada, e por isso nenhum campo que não funciona aparece aqui.
 */
@Component
public class TelaEntrar implements Tela {

    private final Label recado = new Label();
    private final ContextoEmpresa contexto;
    private final Acessos acessos;
    private final Janela janela;

    public TelaEntrar(ContextoEmpresa contexto, Acessos acessos, @Lazy Janela janela) {
        this.contexto = contexto;
        this.acessos = acessos;
        this.janela = janela;
    }

    @Override
    public String secao() {
        return "";
    }

    @Override
    public boolean ocupaTudo() {
        return true;
    }

    @Override
    public Node montar() {
        StackPane fundo = new StackPane(card());
        fundo.getStyleClass().add("fundo-do-portao");
        fundo.setAlignment(Pos.CENTER);
        fundo.setPadding(new Insets(48, 24, 48, 24));
        return fundo;
    }

    /** O card do meio, com tudo dentro. */
    private VBox card() {
        Label sigla = new Label("IT");
        sigla.getStyleClass().add("marca-do-portao");

        Pane nome = TrackedText.bicolor("IT.FC", ItiaFonts.logotype(19),
                ItiaTokens.LOGO_TRACKING, ItiaTokens.BLACK, ".", ItiaTokens.RED);

        VBox marca = new VBox(14, sigla, nome);
        marca.setAlignment(Pos.CENTER);

        Label titulo = new Label("Entrar na sua conta");
        titulo.getStyleClass().add("titulo-do-portao");

        Label chamada = new Label("Use o e-mail cadastrado pela sua empresa.");
        chamada.getStyleClass().add("chamada-do-portao");
        chamada.setWrapText(true);
        chamada.setMinHeight(Region.USE_PREF_SIZE);

        VBox cabeca = new VBox(8, titulo, chamada);
        cabeca.setAlignment(Pos.CENTER);

        recado.getStyleClass().add("recado-portao");
        recado.setWrapText(true);
        recado.setVisible(false);
        recado.setManaged(false);
        recado.setMinHeight(Region.USE_PREF_SIZE);

        TextField email = new TextField();
        email.setPromptText("marina@empresa.com.br");
        TextField quem = new TextField();
        quem.setPromptText("Nome completo");

        Runnable entrar = () -> entrar(quem.getText(), email.getText());
        email.setOnAction(acao -> entrar.run());
        quem.setOnAction(acao -> entrar.run());

        Button botao = Pecas.botao("Entrar", entrar);
        botao.getStyleClass().add("botao-portao");
        botao.setMaxWidth(Double.MAX_VALUE);

        Label aviso = new Label("Acesso ainda sem senha: o nome informado é o que assina cada "
                + "lançamento. A conferência de credenciais entra na próxima etapa.");
        aviso.getStyleClass().add("aviso-sem-trava");
        aviso.setWrapText(true);
        aviso.setMinHeight(Region.USE_PREF_SIZE);

        Label pergunta = new Label("Ainda não tem acesso?");
        pergunta.getStyleClass().add("pe-do-portao");
        Label administrador = new Label("Fale com o administrador");
        administrador.getStyleClass().add("elo-do-portao");

        HBox pe = new HBox(5, pergunta, administrador);
        pe.setAlignment(Pos.CENTER);

        VBox card = new VBox(marca, cabeca, recado,
                campo("E-mail corporativo", email), campo("Nome do operador", quem),
                botao, aviso, pe);
        card.getStyleClass().add("card-do-portao");
        card.setMaxSize(420, Region.USE_PREF_SIZE);
        card.setPrefWidth(420);
        VBox.setMargin(cabeca, new Insets(36, 0, 32, 0));
        VBox.setMargin(recado, new Insets(0, 0, 16, 0));
        VBox.setMargin(botao, new Insets(6, 0, 0, 0));
        VBox.setMargin(aviso, new Insets(18, 0, 0, 0));
        VBox.setMargin(pe, new Insets(24, 0, 0, 0));

        javafx.application.Platform.runLater(email::requestFocus);
        return card;
    }

    /** O rótulo em cima e o campo embaixo, com os 18 de respiro. */
    private VBox campo(String rotulo, TextField campo) {
        Label nome = new Label(rotulo);
        nome.getStyleClass().add("rotulo-do-portao");

        VBox bloco = new VBox(8, nome, campo);
        VBox.setMargin(bloco, new Insets(0, 0, 18, 0));
        return bloco;
    }

    private void entrar(String nome, String email) {
        if (nome == null || nome.isBlank()) {
            recado.setText("Diga o seu nome para entrar. É ele que assina cada lançamento.");
            recado.setVisible(true);
            recado.setManaged(true);
            return;
        }
        contexto.entrar(nome, email);
        contexto.identificar(acessos.identificar(nome, email).getId());
        janela.limparRecados();
        janela.ir(TelaEmpresas.class);
    }
}
