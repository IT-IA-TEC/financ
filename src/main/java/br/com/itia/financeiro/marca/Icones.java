package br.com.itia.financeiro.marca;

import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Os ícones da marca: desenho de traço, no padrão Lucide.
 *
 * Cada ícone é desenhado num quadrado de 24 e reduzido para o tamanho pedido.
 * O traço é sempre 1.5 e a cor vem de fora, para o mesmo ícone servir no menu
 * escuro e no conteúdo claro. Não existe emoji em lugar nenhum do sistema.
 */
public final class Icones {

    /** O traço do desenho, como manda a identidade. */
    private static final double TRACO = 1.5;

    private Icones() {
    }

    public static StackPane painel(double lado, Color cor) {
        return de(lado, cor, "M3 3h7v7H3z", "M14 3h7v7h-7z", "M14 14h7v7h-7z", "M3 14h7v7H3z");
    }

    public static StackPane tarefas(double lado, Color cor) {
        return de(lado, cor, "M9 11l3 3L22 4",
                "M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11");
    }

    public static StackPane receber(double lado, Color cor) {
        return de(lado, cor, "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z", "M8 12l4 4 4-4",
                "M12 8v8");
    }

    public static StackPane pagar(double lado, Color cor) {
        return de(lado, cor, "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z", "M16 12l-4-4-4 4",
                "M12 16V8");
    }

    public static StackPane conciliacao(double lado, Color cor) {
        return de(lado, cor, "M8 3 4 7l4 4", "M4 7h16", "M16 21l4-4-4-4", "M20 17H4");
    }

    public static StackPane cobranca(double lado, Color cor) {
        return de(lado, cor, "M3 11v2a1 1 0 0 0 1 1h3l6 4V6L7 10H4a1 1 0 0 0-1 1z",
                "M18 8a5 5 0 0 1 0 8");
    }

    public static StackPane clientes(double lado, Color cor) {
        return de(lado, cor, "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2",
                "M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z", "M22 21v-2a4 4 0 0 0-3-3.87");
    }

    public static StackPane pacotes(double lado, Color cor) {
        return de(lado, cor, "M21 16V8l-9-5-9 5v8l9 5 9-5z", "M3.3 7.3 12 12l8.7-4.7",
                "M12 12v10");
    }

    public static StackPane integracoes(double lado, Color cor) {
        return de(lado, cor, "M10 13a5 5 0 0 0 7 0l3-3a5 5 0 0 0-7-7l-1 1",
                "M14 11a5 5 0 0 0-7 0l-3 3a5 5 0 0 0 7 7l1-1");
    }

    public static StackPane inteligencia(double lado, Color cor) {
        return de(lado, cor, "M12 3l1.9 4.6 4.6 1.9-4.6 1.9L12 16l-1.9-4.6L5.5 9.5l4.6-1.9z",
                "M19 15l.7 1.8 1.8.7-1.8.7-.7 1.8-.7-1.8-1.8-.7 1.8-.7z");
    }

    public static StackPane tags(double lado, Color cor) {
        return de(lado, cor, "M20.6 13.4 12 22l-9-9V4h9l8.6 8.6a2 2 0 0 1 0 2.8z",
                "M7.5 7.5h.01");
    }

    public static StackPane busca(double lado, Color cor) {
        return de(lado, cor, "M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14z", "M20 20l-4.35-4.35");
    }

    public static StackPane sino(double lado, Color cor) {
        return de(lado, cor, "M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9",
                "M13.73 21a2 2 0 0 1-3.46 0");
    }

    public static StackPane empresa(double lado, Color cor) {
        return de(lado, cor, "M3 21h18", "M5 21V7l7-4 7 4v14", "M9 9h.01", "M9 13h.01",
                "M9 17h.01", "M15 9h.01", "M15 13h.01", "M15 17h.01");
    }

    public static StackPane olhoAberto(double lado, Color cor) {
        return de(lado, cor, "M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7z",
                "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z");
    }

    public static StackPane olhoFechado(double lado, Color cor) {
        return de(lado, cor, "M9.9 4.2A10.9 10.9 0 0 1 12 4c6.4 0 10 7 10 7a18 18 0 0 1-2.7 3.7",
                "M6.6 6.6A18 18 0 0 0 2 11s3.6 7 10 7a10.7 10.7 0 0 0 5.4-1.4",
                "M9.9 9.9a3 3 0 0 0 4.2 4.2", "M2 2l20 20");
    }

    public static StackPane voltar(double lado, Color cor) {
        return de(lado, cor, "M19 12H5", "M12 19l-7-7 7-7");
    }

    public static StackPane recolher(double lado, Color cor) {
        return de(lado, cor, "M11 17l-5-5 5-5", "M18 17l-5-5 5-5");
    }

    public static StackPane abrir(double lado, Color cor) {
        return de(lado, cor, "M13 17l5-5-5-5", "M6 17l5-5-5-5");
    }

    /** O ícone de uma parte do menu, pelo nome da seção. */
    public static StackPane daSecao(String secao, double lado, Color cor) {
        return switch (secao) {
            case "cupula" -> tarefas(lado, cor);
            case "titulos" -> receber(lado, cor);
            case "pagar" -> pagar(lado, cor);
            case "conciliacao" -> conciliacao(lado, cor);
            case "cobranca" -> cobranca(lado, cor);
            case "clientes" -> clientes(lado, cor);
            case "servicos" -> pacotes(lado, cor);
            case "integracoes" -> integracoes(lado, cor);
            case "inteligencia" -> inteligencia(lado, cor);
            case "etiquetas" -> tags(lado, cor);
            default -> painel(lado, cor);
        };
    }

    /** Monta o desenho e encolhe do quadrado de 24 para o tamanho pedido. */
    private static StackPane de(double lado, Color cor, String... partes) {
        Group desenho = new Group();
        for (String parte : partes) {
            SVGPath traco = new SVGPath();
            traco.setContent(parte);
            traco.setFill(null);
            traco.setStroke(cor);
            traco.setStrokeWidth(TRACO);
            traco.setStrokeLineCap(StrokeLineCap.ROUND);
            traco.setStrokeLineJoin(StrokeLineJoin.ROUND);
            desenho.getChildren().add(traco);
        }
        double escala = lado / 24.0;
        desenho.setScaleX(escala);
        desenho.setScaleY(escala);
        // o desenho por si não reserva espaço: a caixa fixa segura o lugar dele
        StackPane caixa = new StackPane(desenho);
        caixa.setMinSize(lado, lado);
        caixa.setPrefSize(lado, lado);
        caixa.setMaxSize(lado, lado);
        return caixa;
    }
}
