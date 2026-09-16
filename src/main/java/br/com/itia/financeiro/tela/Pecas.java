package br.com.itia.financeiro.tela;

import br.com.itia.financeiro.marca.Eyebrow;
import br.com.itia.financeiro.marca.ItiaTokens;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * As peças do padrão de sistema da IT, prontas para qualquer tela usar.
 *
 * Existe para que nenhuma tela invente forma própria: o cabeçalho, o quadro de
 * número, a seção, a tabela e o botão são sempre os mesmos, do mesmo tamanho e
 * com a mesma cor. É o que faz o sistema parecer um só.
 */
public final class Pecas {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_CURTO = DateTimeFormatter.ofPattern("dd/MM");

    private Pecas() {
    }

    // ------------------------------------------------------------- cabeçalho

    /** O cabeçalho da página: olho vermelho, título e a linha de apoio. */
    public static VBox cabecalho(String olho, String titulo, String apoio, Node... acoes) {
        HBox linhaDoOlho = Eyebrow.build(olho, ItiaTokens.ACCENT, true);

        Label nome = new Label(titulo);
        nome.getStyleClass().add("titulo-pagina");
        // o nome da tela nunca é cortado: ele segura a própria largura
        nome.setMinWidth(Region.USE_PREF_SIZE);

        VBox esquerda = new VBox(4, linhaDoOlho, nome);
        if (apoio != null && !apoio.isBlank()) {
            Label linha = new Label(apoio);
            linha.getStyleClass().add("linha-apoio");
            linha.setWrapText(true);
            esquerda.getChildren().add(linha);
        }

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);

        // a fileira de números vem antes dos botões, como na web. Quando são
        // muitos cartões, ela desce para uma linha própria: assim nenhum cartão
        // passa por cima do título nem escapa da borda do card
        java.util.List<Node> emOrdem = new java.util.ArrayList<>();
        Node fileiraGrande = null;
        for (Node acao : acoes) {
            if (acao != null && acao.getStyleClass().contains("quadros-do-topo")) {
                boolean muitos = acao instanceof javafx.scene.Parent pai
                        && pai.getChildrenUnmodifiable().size() > 4;
                if (muitos) {
                    fileiraGrande = acao;
                } else {
                    emOrdem.add(acao);
                }
            }
        }
        for (Node acao : acoes) {
            if (acao != null && !acao.getStyleClass().contains("quadros-do-topo")) {
                emOrdem.add(acao);
            }
        }

        // se não couber na largura, a fileira quebra para a linha de baixo:
        // nada do cabeçalho sai da tela
        javafx.scene.layout.FlowPane direita = new javafx.scene.layout.FlowPane(12, 10,
                emOrdem.toArray(new Node[0]));
        direita.setAlignment(Pos.BOTTOM_RIGHT);
        direita.setColumnHalignment(javafx.geometry.HPos.RIGHT);
        direita.setMinWidth(0);

        // o texto da esquerda não empurra os botões para fora
        esquerda.setMaxWidth(560);
        esquerda.setMinWidth(0);
        HBox.setHgrow(direita, Priority.ALWAYS);

        HBox faixa = new HBox(16, esquerda, espaco, direita);
        faixa.setAlignment(Pos.BOTTOM_LEFT);
        faixa.setMinWidth(0);

        // a direita nunca passa por cima do texto: ela só pode ocupar o que
        // sobra depois do título, e o que não couber desce para a linha de baixo
        direita.maxWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> Math.max(0, faixa.getWidth() - esquerda.getWidth() - 32),
                faixa.widthProperty(), esquerda.widthProperty()));

        VBox caixa = new VBox(14, faixa);
        caixa.getStyleClass().add("cabecalho-pagina");
        if (fileiraGrande != null) {
            caixa.getChildren().add(emLinhaQueQuebra(fileiraGrande));
        }
        return caixa;
    }

    /**
     * A fileira grande de cartões vira um painel que quebra sozinho: o que não
     * cabe na largura desce para a linha de baixo, sem nunca sair do card.
     */
    private static javafx.scene.layout.FlowPane emLinhaQueQuebra(Node fileira) {
        javafx.scene.layout.FlowPane painel = new javafx.scene.layout.FlowPane(12, 10);
        painel.setMinWidth(0);
        if (fileira instanceof javafx.scene.layout.Pane antiga) {
            java.util.List<Node> cartoes =
                    new java.util.ArrayList<>(antiga.getChildren());
            antiga.getChildren().clear();
            for (Node cartao : cartoes) {
                if (cartao instanceof Region caixa) {
                    caixa.setMinWidth(196);
                    caixa.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    caixa.setMaxWidth(Region.USE_PREF_SIZE);
                }
                painel.getChildren().add(cartao);
            }
        }
        return painel;
    }

    /** O título de uma seção, com o fio preto embaixo. */
    public static Label secao(String nome) {
        Label titulo = new Label(nome);
        titulo.getStyleClass().add("titulo-secao");
        titulo.setMaxWidth(Double.MAX_VALUE);
        return titulo;
    }

    // --------------------------------------------------------------- quadros

    /** A fileira de números do topo da tela. O primeiro vem no preto. */
    public static HBox quadros(Node... quadros) {
        HBox fileira = new HBox(12, quadros);
        for (Node quadro : quadros) {
            HBox.setHgrow(quadro, Priority.ALWAYS);
        }
        return fileira;
    }

    public static VBox quadro(String rotulo, String numero, String apoio) {
        return quadro(rotulo, numero, apoio, false, false);
    }

    /**
     * A fileira de números que fica na mesma linha do cabeçalho, como na web:
     * quadros estreitos, colados à direita do título.
     */
    public static HBox quadrosDoTopo(Node... quadros) {
        HBox fileira = new HBox(12, quadros);
        fileira.getStyleClass().add("quadros-do-topo");
        fileira.setAlignment(Pos.BOTTOM_LEFT);
        for (int onde = 0; onde < quadros.length; onde++) {
            if (quadros[onde] instanceof Region cartao) {
                // largura mínima de 230: o cartão cresce com o número, não corta
                cartao.setMinWidth(196);
                cartao.setPrefWidth(Region.USE_COMPUTED_SIZE);
                HBox.setHgrow(cartao, Priority.ALWAYS);
            }
        }
        return fileira;
    }

    /** O quadro com a borda vermelha, de quando o número pede atenção. */
    public static VBox quadroDeAtencao(String rotulo, String numero, String apoio) {
        VBox caixa = quadro(rotulo, numero, apoio, false, true);
        caixa.getStyleClass().add("atencao");
        return caixa;
    }

    /** O último quadro da fileira, com o fundo listrado da marca. */
    public static VBox quadroListrado(String rotulo, String numero, String apoio) {
        VBox caixa = quadro(rotulo, numero, apoio);
        caixa.getStyleClass().add("listrado");
        return caixa;
    }

    public static VBox quadro(String rotulo, String numero, String apoio, boolean forte,
                              boolean vermelho) {
        Label r = new Label(rotulo.toUpperCase());
        r.getStyleClass().add("rotulo");

        Label n = new Label(numero);
        n.getStyleClass().add("numero");
        if (vermelho) {
            n.getStyleClass().add("vermelho");
        }

        VBox caixa = new VBox(4, r, n);
        if (apoio != null && !apoio.isBlank()) {
            Label a = new Label(apoio);
            a.getStyleClass().add("apoio");
            caixa.getChildren().add(a);
        }
        caixa.getStyleClass().add("quadro");
        if (forte) {
            caixa.getStyleClass().add("forte");
        }
        if (vermelho) {
            // número em vermelho pede também a borda vermelha, como na web
            caixa.getStyleClass().add("atencao");
        }
        return caixa;
    }

    /**
     * Põe uma coluna para rolar por dentro.
     *
     * Usado nas telas de mesa: a coluna ocupa a altura que tem e o conteúdo que
     * passa disso rola ali mesmo, sem empurrar a tela inteira para baixo.
     */
    public static javafx.scene.control.ScrollPane rolandoPorDentro(Node dentro) {
        javafx.scene.control.ScrollPane rolagem = new javafx.scene.control.ScrollPane(dentro);
        rolagem.setFitToWidth(true);
        rolagem.getStyleClass().add("rolagem-coluna");
        rolagem.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(rolagem, Priority.ALWAYS);
        return rolagem;
    }

    // ---------------------------------------------------------------- botões

    public static Button botao(String texto, Runnable acao) {
        Button botao = new Button(texto);
        botao.getStyleClass().add("botao");
        // o nome do botão nunca é cortado: ele mantém a largura do texto
        botao.setMinWidth(Region.USE_PREF_SIZE);
        botao.setOnAction(clique -> acao.run());
        return botao;
    }

    public static Button botaoVazado(String texto, Runnable acao) {
        Button botao = botao(texto, acao);
        botao.getStyleClass().add("vazado");
        return botao;
    }

    public static Button botaoPerigo(String texto, Runnable acao) {
        Button botao = botao(texto, acao);
        botao.getStyleClass().add("perigo");
        return botao;
    }

    // ------------------------------------------------------------------ abas

    /**
     * A faixa preta de abas de um módulo, igual à que o sistema já usava para
     * separar as partes de Contas a receber, Cobrança e Pacotes.
     */
    public static HBox abas(String atual, java.util.LinkedHashMap<String, Runnable> partes) {
        HBox faixa = new HBox(0);
        faixa.getStyleClass().add("abas");
        faixa.setAlignment(Pos.CENTER_LEFT);
        for (java.util.Map.Entry<String, Runnable> parte : partes.entrySet()) {
            Label aba = new Label(parte.getKey());
            aba.getStyleClass().add("aba");
            if (parte.getKey().equalsIgnoreCase(atual)) {
                aba.getStyleClass().add("ativa");
            }
            aba.setOnMouseClicked(clique -> parte.getValue().run());
            faixa.getChildren().add(aba);

            // entre uma aba e outra vai o traço vermelho da marca
            Label traco = new Label("-");
            traco.getStyleClass().add("traco-aba");
            faixa.getChildren().add(traco);
        }
        // a última aba não leva separador depois dela
        if (!faixa.getChildren().isEmpty()) {
            faixa.getChildren().remove(faixa.getChildren().size() - 1);
        }
        return faixa;
    }

    // ----------------------------------------------------------------- vazio

    public static VBox vazio(String texto) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("texto");
        rotulo.setWrapText(true);
        rotulo.setMaxWidth(620);

        VBox caixa = new VBox(rotulo);
        caixa.getStyleClass().add("vazio");
        caixa.setAlignment(Pos.CENTER);
        return caixa;
    }

    /** Um aviso destacado, com o traço âmbar do que pede atenção. */
    public static VBox aviso(String texto) {
        Label recado = new Label(texto);
        recado.setWrapText(true);
        VBox caixa = new VBox(recado);
        caixa.getStyleClass().add("caixa-aviso");
        caixa.setPadding(new Insets(14, 18, 14, 18));
        return caixa;
    }

    public static VBox caixa(Node... dentro) {
        VBox caixa = new VBox(14, dentro);
        caixa.getStyleClass().add("caixa");
        return caixa;
    }

    /** Um campo de formulário: o rótulo em cima, o campo embaixo. */
    public static VBox campo(String rotulo, Node campo) {
        Label nome = new Label(rotulo);
        nome.getStyleClass().add("rotulo-campo");
        VBox caixa = new VBox(5, nome, campo);
        VBox.setVgrow(campo, Priority.NEVER);
        return caixa;
    }

    // --------------------------------------------------------------- números

    /** Dinheiro do jeito daqui: R$ 1.234,56. */
    public static String dinheiro(BigDecimal valor) {
        BigDecimal certo = valor == null ? BigDecimal.ZERO : valor;
        return "R$ " + String.format(Locale.forLanguageTag("pt-BR"), "%,.2f", certo);
    }

    /** O número sem o "R$", para dentro de tabela. */
    public static String numero(BigDecimal valor) {
        BigDecimal certo = valor == null ? BigDecimal.ZERO : valor;
        return String.format(Locale.forLanguageTag("pt-BR"), "%,.2f", certo);
    }

    public static String data(LocalDate dia) {
        return dia == null ? "" : dia.format(DIA);
    }

    public static String dataCurta(LocalDate dia) {
        return dia == null ? "" : dia.format(DIA_CURTO);
    }

    /** Espaço que empurra o que vem depois para o outro lado. */
    public static Region empurrar() {
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        return espaco;
    }

    public static Insets folga() {
        return new Insets(20, 24, 24, 24);
    }
}
