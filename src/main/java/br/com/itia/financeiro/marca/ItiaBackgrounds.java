package br.com.itia.financeiro.marca;

import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * As duas texturas da marca e o gradiente de protecao, com a mesma matematica do CSS.
 * Grade: #050506 com linhas 1px #151517 em modulo de 64px  ->  1px / 64px = 1.5625%.
 * Listras: 45deg, bandas de 10px (periodo 20px)  ->  eixo from 0px 0px to 14.142px 14.142px, paradas em 50%.
 */
public final class ItiaBackgrounds {
    private ItiaBackgrounds() {}

    public static final String GRID =
            "-fx-background-color:"
            + "#050506,"
            + "repeating-linear-gradient(from 0px 0px to 0px 64px, #151517 0%, #151517 1.5625%, transparent 1.5625%, transparent 100%),"
            + "repeating-linear-gradient(from 0px 0px to 64px 0px, #151517 0%, #151517 1.5625%, transparent 1.5625%, transparent 100%);";

    public static final String STRIPES =
            "-fx-background-color: repeating-linear-gradient(from 0px 0px to 14.142px 14.142px,"
            + " #151517 0%, #151517 50%, #050506 50%, #050506 100%);";

    public static final String STRIPES_SOFT =
            "-fx-background-color: repeating-linear-gradient(from 0px 0px to 14.142px 14.142px,"
            + " #2E2E31 0%, #2E2E31 50%, #151517 50%, #151517 100%);";

    /** Brilho radial branco 6% das capas escuras: ellipse 70% 60% at 50% 40%. */
    public static final String GLOW =
            "-fx-background-color: radial-gradient(center 50% 40%, radius 60%,"
            + " rgba(255,255,255,0.06) 0%, rgba(5,5,6,0.0) 65%);";

    /** Gradiente de protecao sobre foto: rgba(5,5,6,.95) -> .6 em 35% -> 0 em 70%, de baixo para cima. */
    public static final String PROTECTION =
            "-fx-background-color: linear-gradient(to top,"
            + " rgba(5,5,6,0.95) 0%, rgba(5,5,6,0.6) 35%, rgba(5,5,6,0.0) 70%);";

    public static Region grid()        { Region r = new Region(); r.setStyle(GRID); return r; }
    public static Region stripes()     { Region r = new Region(); r.setStyle(STRIPES); return r; }
    public static Region stripesSoft() { Region r = new Region(); r.setStyle(STRIPES_SOFT); return r; }
    public static Region glow()        { Region r = new Region(); r.setStyle(GLOW); return r; }
    public static Region protection()  { Region r = new Region(); r.setStyle(PROTECTION); return r; }

    /** Grade + brilho empilhados, como nas capas e no slide de titulo. */
    public static StackPane gridWithGlow() {
        StackPane stack = new StackPane(grid(), glow());
        stack.setStyle(GRID);
        return stack;
    }

    /** Marcas de canto 22px, 1px Nevoa, inset 40px — moldura das capas escuras. */
    public static Pane cornerMarks(double width, double height, double inset, double arm) {
        Pane pane = new Pane();
        pane.setPickOnBounds(false);
        pane.setPrefSize(width, height);
        String mist = "#B9B9BE";
        pane.getChildren().addAll(
                corner(inset, inset, arm, "1px 0 0 1px", mist),
                corner(width - inset - arm, inset, arm, "1px 1px 0 0", mist),
                corner(inset, height - inset - arm, arm, "0 0 1px 1px", mist),
                corner(width - inset - arm, height - inset - arm, arm, "0 1px 1px 0", mist));
        return pane;
    }

    private static Region corner(double x, double y, double arm, String widths, String color) {
        Region r = new Region();
        r.setPrefSize(arm, arm);
        r.setMinSize(arm, arm);
        r.setMaxSize(arm, arm);
        r.setLayoutX(x);
        r.setLayoutY(y);
        r.setStyle("-fx-border-color:" + color + ";-fx-border-width:" + widths + ";-fx-background-color:transparent;");
        return r;
    }
}
