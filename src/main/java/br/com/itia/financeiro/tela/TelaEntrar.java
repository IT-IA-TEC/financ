package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.marca.Eyebrow;
import br.com.itia.financeiro.marca.ItiaFonts;
import br.com.itia.financeiro.marca.ItiaTokens;
import br.com.itia.financeiro.marca.TrackedText;
import br.com.itia.financeiro.servico.Acessos;
import br.com.itia.financeiro.servico.ContextoEmpresa;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * A porta de entrada, igual à de sempre.
 *
 * Duas metades: à esquerda o que o sistema é, no preto com a grade da marca; à
 * direita a identificação de quem vai trabalhar. As medidas são as mesmas do
 * desenho original: 64 por 56 de folga, título de 40, fio de 3 no topo de cada
 * pilar, sendo o primeiro vermelho, e o formulário com 380 de largura.
 *
 * Ainda sem senha: a conferência de credenciais entra na etapa combinada.
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
        GridPane portao = new GridPane();
        ColumnConstraints esquerda = new ColumnConstraints();
        esquerda.setPercentWidth(50);
        esquerda.setHgrow(Priority.ALWAYS);
        ColumnConstraints direita = new ColumnConstraints();
        direita.setPercentWidth(50);
        direita.setHgrow(Priority.ALWAYS);
        portao.getColumnConstraints().addAll(esquerda, direita);

        RowConstraints altura = new RowConstraints();
        altura.setVgrow(Priority.ALWAYS);
        portao.getRowConstraints().add(altura);

        portao.add(ladoEscuro(), 0, 0);
        portao.add(ladoClaro(), 1, 0);
        return portao;
    }

    /** O lado preto, com a grade de 64 em 64 da marca. */
    private StackPane ladoEscuro() {
        Pane marca = TrackedText.bicolor("IT.FC", ItiaFonts.logotype(26),
                ItiaTokens.LOGO_TRACKING, ItiaTokens.WHITE, ".", ItiaTokens.RED);

        Label tese = new Label("Contas a receber com segregação total por empresa.");
        tese.getStyleClass().add("tese");
        tese.setWrapText(true);
        tese.setMaxWidth(430);
        tese.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        Label explicacao = new Label("Carteira, faturamento e conciliação operados em bases "
                + "isoladas, sob a mesma plataforma. Escala de uma empresa a um grupo inteiro "
                + "sem duplicar estrutura.");
        explicacao.getStyleClass().add("tese-apoio");
        explicacao.setWrapText(true);
        explicacao.setMaxWidth(360);
        explicacao.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        VBox blocoDaTese = new VBox(18, Eyebrow.build("GESTÃO FINANCEIRA", ItiaTokens.TEXT_ON_INV_MUTED, true), tese,
                explicacao);

        VBox pilares = new VBox(22,
                pilar("01", "Faturamento",
                        "Emissão com numeração sequencial e identificador de cobrança por "
                                + "título.", true),
                pilar("02", "Conciliação",
                        "Baixa por identificador de transação, com bloqueio de duplicidade e "
                                + "tolerância parametrizada.", false),
                pilar("03", "Auditoria",
                        "Trilha completa de lançamentos, com autor, data e valor de cada "
                                + "movimentação.", false));

        Label rodape = new Label("IT.IA · IT.FC");
        rodape.getStyleClass().add("rodape-portao");

        javafx.scene.layout.Region espaco = new javafx.scene.layout.Region();
        VBox.setVgrow(espaco, Priority.ALWAYS);

        VBox conteudo = new VBox(56, marca, blocoDaTese, pilares, espaco, rodape);
        conteudo.setPadding(new Insets(64, 56, 64, 56));

        // a grade da marca: fios de 1px a cada 64px, no carvão
        Canvas grade = new Canvas();
        StackPane lado = new StackPane(grade, conteudo);
        lado.getStyleClass().add("lado-escuro");
        grade.widthProperty().bind(lado.widthProperty());
        grade.heightProperty().bind(lado.heightProperty());
        grade.widthProperty().addListener((onde, antes, agora) -> desenharGrade(grade));
        grade.heightProperty().addListener((onde, antes, agora) -> desenharGrade(grade));
        return lado;
    }


    private VBox pilar(String numero, String nome, String texto, boolean primeiro) {
        Label num = new Label(numero);
        num.getStyleClass().add("num-pilar");

        Label titulo = new Label(nome);
        titulo.getStyleClass().add("nome-pilar");

        Label linha = new Label(texto);
        linha.getStyleClass().add("texto-pilar");
        linha.setWrapText(true);
        linha.setMaxWidth(380);
        linha.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        VBox bloco = new VBox(3, num, titulo, linha);
        bloco.getStyleClass().add("pilar");
        if (primeiro) {
            bloco.getStyleClass().add("primeiro");
        }
        bloco.setMaxWidth(380);
        return bloco;
    }

    private void desenharGrade(Canvas grade) {
        GraphicsContext pincel = grade.getGraphicsContext2D();
        pincel.setFill(Color.web("#050506"));
        pincel.fillRect(0, 0, grade.getWidth(), grade.getHeight());
        pincel.setStroke(Color.web("#151517"));
        pincel.setLineWidth(1);
        for (double x = 0.5; x < grade.getWidth(); x += 64) {
            pincel.strokeLine(x, 0, x, grade.getHeight());
        }
        for (double y = 0.5; y < grade.getHeight(); y += 64) {
            pincel.strokeLine(0, y, grade.getWidth(), y);
        }
    }

    /** O lado claro: o formulário de 380 de largura, centralizado. */
    private VBox ladoClaro() {
        recado.getStyleClass().add("recado-portao");
        recado.setWrapText(true);
        recado.setMaxWidth(380);
        recado.setVisible(false);
        recado.setManaged(false);

        Label titulo = new Label("Entrar");
        titulo.getStyleClass().add("titulo-portao");

        Label chamada = new Label("Identificação do operador. O nome informado assina cada "
                + "lançamento.");
        chamada.getStyleClass().add("chamada");
        chamada.setWrapText(true);
        chamada.setMaxWidth(380);
        chamada.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        TextField nome = new TextField();
        nome.setPromptText("Nome completo");
        TextField email = new TextField();
        email.setPromptText("nome@empresa.com.br");

        Runnable entrar = () -> entrar(nome.getText(), email.getText());
        nome.setOnAction(acao -> entrar.run());
        email.setOnAction(acao -> entrar.run());

        Button botao = Pecas.botao("Entrar", entrar);
        botao.getStyleClass().add("botao-portao");
        botao.setMaxWidth(Double.MAX_VALUE);

        Label aviso = new Label("Acesso ainda sem senha. A conferência de credenciais entra na "
                + "próxima etapa.");
        aviso.getStyleClass().add("aviso-sem-trava");
        aviso.setWrapText(true);
        aviso.setMaxWidth(380);
        aviso.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        VBox formulario = new VBox(Eyebrow.build("ACESSO", ItiaTokens.ACCENT, true), titulo, chamada, recado,
                campo("Nome do operador", nome), campo("E-mail", email), botao, aviso);
        VBox.setMargin(titulo, new Insets(12, 0, 0, 0));
        VBox.setMargin(chamada, new Insets(8, 0, 28, 0));
        VBox.setMargin(recado, new Insets(0, 0, 16, 0));
        VBox.setMargin(aviso, new Insets(16, 0, 0, 0));
        formulario.setMaxWidth(380);
        formulario.setPrefWidth(380);

        VBox lado = new VBox(formulario);
        lado.getStyleClass().add("lado-claro");
        lado.setAlignment(Pos.CENTER);
        lado.setPadding(new Insets(64, 56, 64, 56));
        javafx.application.Platform.runLater(nome::requestFocus);
        return lado;
    }

    /** O rótulo em caixa alta e o campo, com os 16 de respiro embaixo. */
    private VBox campo(String rotulo, TextField campo) {
        Label nome = new Label(rotulo.toUpperCase());
        nome.getStyleClass().add("rotulo-portao");

        VBox bloco = new VBox(6, nome, campo);
        VBox.setMargin(bloco, new Insets(0, 0, 16, 0));
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
