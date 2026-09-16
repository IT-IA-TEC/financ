package br.com.itia.financeiro.tela;

import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A tabela do padrão da IT: cabeçalho preto, linhas zebradas, fio claro entre
 * elas, número alinhado à direita.
 *
 * É montada linha por linha, igual à tabela que o sistema sempre teve nas
 * páginas. Mostra tudo de uma vez, sem barra de rolagem por dentro: quem rola é
 * a tela, e assim a leitura não fica presa numa janelinha.
 *
 * Uso:
 *
 *   Tabela.de(titulos)
 *       .coluna("Nº", t -> t.getNumero())
 *       .valor("Saldo", t -> dinheiro(t.getSaldo()))
 *       .aoClicar(t -> abrir(t))
 *       .montar()
 */
public final class Tabela<T> {

    private record Coluna<T>(String titulo, Function<T, String> leitura, double peso,
                             boolean numero) {
    }

    private final List<T> itens;
    private final List<Coluna<T>> colunas = new ArrayList<>();
    private Consumer<T> aoClicar;
    private final java.util.Map<String, Runnable> funis = new java.util.LinkedHashMap<>();
    private final java.util.Set<String> funisLigados = new java.util.HashSet<>();
    private Function<T, String> marcaDaLinha;
    private Function<T, String> corDaMarca;
    private String textoQuandoVazia = "Nada aqui ainda.";

    private Tabela(List<T> itens) {
        this.itens = itens;
    }

    public static <T> Tabela<T> de(List<T> itens) {
        return new Tabela<>(itens);
    }

    public Tabela<T> coluna(String titulo, Function<T, String> leitura) {
        return coluna(titulo, leitura, 1);
    }

    /** O peso diz quanto essa coluna ocupa em relação às outras. */
    public Tabela<T> coluna(String titulo, Function<T, String> leitura, double peso) {
        colunas.add(new Coluna<>(titulo, leitura, peso, false));
        return this;
    }

    /** Coluna de dinheiro ou quantidade: alinhada à direita. */
    public Tabela<T> valor(String titulo, Function<T, String> leitura) {
        colunas.add(new Coluna<>(titulo, leitura, 1, true));
        return this;
    }

    /**
     * Põe o funil de filtro no cabeçalho desta coluna. Quando a coluna está
     * filtrando, o funil aparece aceso.
     */
    public Tabela<T> funil(String titulo, boolean ligado, Runnable aoClicar) {
        funis.put(titulo, aoClicar);
        if (ligado) {
            funisLigados.add(titulo);
        }
        return this;
    }

    public Tabela<T> aoClicar(Consumer<T> acao) {
        this.aoClicar = acao;
        return this;
    }

    /** Uma marca de situação no fim da linha (vencido, pago, cancelado). */
    public Tabela<T> comMarca(Function<T, String> marca) {
        return comMarca(marca, item -> "");
    }

    /**
     * A marca com a cor da situação: vermelho para vencido, preto para pago,
     * apagado para cancelado. A cor vem em "s-vencido", "s-pago" e assim por
     * diante, igual ao que o sistema já usava.
     */
    public Tabela<T> comMarca(Function<T, String> marca, Function<T, String> cor) {
        this.marcaDaLinha = marca;
        this.corDaMarca = cor;
        return this;
    }

    public Tabela<T> quandoVazia(String texto) {
        this.textoQuandoVazia = texto;
        return this;
    }

    public Node montar() {
        if (itens.isEmpty()) {
            return Pecas.vazio(textoQuandoVazia);
        }

        GridPane grade = new GridPane();
        grade.getStyleClass().add("tabela");

        double total = colunas.stream().mapToDouble(Coluna::peso).sum()
                + (marcaDaLinha == null ? 0 : 1);
        for (Coluna<T> coluna : colunas) {
            ColumnConstraints largura = new ColumnConstraints();
            largura.setPercentWidth(100 * coluna.peso() / total);
            largura.setHgrow(Priority.ALWAYS);
            // a coluna encolhe em vez de empurrar a tela para o lado
            largura.setMinWidth(10);
            largura.setHalignment(coluna.numero() ? HPos.RIGHT : HPos.LEFT);
            grade.getColumnConstraints().add(largura);
        }
        if (marcaDaLinha != null) {
            ColumnConstraints largura = new ColumnConstraints();
            largura.setPercentWidth(100 / total);
            largura.setMinWidth(10);
            largura.setHalignment(HPos.LEFT);
            grade.getColumnConstraints().add(largura);
        }

        int coluna = 0;
        for (Coluna<T> titulo : colunas) {
            grade.add(cabecalho(titulo.titulo(), titulo.numero()), coluna++, 0);
        }
        if (marcaDaLinha != null) {
            grade.add(cabecalho("Situação", false), coluna, 0);
        }

        int numeroDaLinha = 1;
        for (T item : itens) {
            coluna = 0;
            List<Node> celulas = new ArrayList<>();
            for (Coluna<T> daColuna : colunas) {
                Node celula = celula(daColuna.leitura().apply(item), daColuna.numero(),
                        numeroDaLinha);
                celulas.add(celula);
                grade.add(celula, coluna++, numeroDaLinha);
            }
            if (marcaDaLinha != null) {
                Node marca = marca(marcaDaLinha.apply(item),
                        corDaMarca == null ? "" : corDaMarca.apply(item), numeroDaLinha);
                celulas.add(marca);
                grade.add(marca, coluna, numeroDaLinha);
            }
            if (aoClicar != null) {
                for (Node celula : celulas) {
                    celula.setStyle(celula.getStyle() + "-fx-cursor: hand;");
                    celula.setOnMouseClicked(clique -> aoClicar.accept(item));
                }
            }
            numeroDaLinha++;
        }
        return grade;
    }

    private Node cabecalho(String texto, boolean numero) {
        Label rotulo = new Label(texto.toUpperCase());
        rotulo.getStyleClass().add("celula-cabecalho");
        rotulo.setMaxWidth(Double.MAX_VALUE);
        rotulo.setMinWidth(0);
        rotulo.setAlignment(numero ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Runnable aoFiltrar = funis.get(texto);
        if (aoFiltrar == null) {
            return rotulo;
        }
        // dentro da caixa quem pinta o fundo é a caixa, senão a folga vem dobrada
        rotulo.getStyleClass().remove("celula-cabecalho");
        rotulo.getStyleClass().add("texto-cabecalho");
        Label funil = new Label("▼");
        funil.getStyleClass().add("funil");
        if (funisLigados.contains(texto)) {
            funil.getStyleClass().add("aceso");
        }
        funil.setOnMouseClicked(clique -> aoFiltrar.run());

        javafx.scene.layout.HBox caixa = new javafx.scene.layout.HBox(6, rotulo, funil);
        caixa.getStyleClass().add("celula-cabecalho");
        caixa.setAlignment(numero ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        caixa.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.layout.HBox.setHgrow(rotulo, Priority.ALWAYS);
        return caixa;
    }

    private Label celula(String texto, boolean numero, int linha) {
        Label rotulo = new Label(texto == null ? "" : texto);
        rotulo.getStyleClass().add("celula");
        if (numero) {
            rotulo.getStyleClass().add("valor");
        }
        if (linha % 2 == 0) {
            rotulo.getStyleClass().add("par");
        }
        rotulo.setMaxWidth(Double.MAX_VALUE);
        rotulo.setMinWidth(0);
        rotulo.setAlignment(numero ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return rotulo;
    }

    private VBox marca(String texto, String cor, int linha) {
        Label marca = new Label(texto == null ? "" : texto);
        marca.getStyleClass().add("marca-situacao");
        if (cor != null && !cor.isBlank()) {
            marca.getStyleClass().add(cor);
        }

        VBox caixa = new VBox(marca);
        caixa.setMinWidth(0);
        caixa.getStyleClass().add("celula");
        if (linha % 2 == 0) {
            caixa.getStyleClass().add("par");
        }
        caixa.setAlignment(Pos.CENTER_LEFT);
        caixa.setMaxWidth(Double.MAX_VALUE);
        return caixa;
    }
}
