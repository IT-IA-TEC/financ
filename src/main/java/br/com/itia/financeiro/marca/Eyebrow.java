package br.com.itia.financeiro.marca;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Rotulo de secao: ponto vermelho de 7px + texto em caixa alta com tracking 0.2em, gap 14px.
 * Equivalente a components/brand/Eyebrow.jsx.
 */
public final class Eyebrow {
    private Eyebrow() {}

    public static HBox of(String text) { return build(text, ItiaTokens.TEXT_SECONDARY, true); }

    public static HBox inverse(String text) { return build(text, ItiaTokens.TEXT_ON_INV_MUTED, true); }

    public static HBox withoutDot(String text) { return build(text, ItiaTokens.TEXT_SECONDARY, false); }

    public static HBox build(String text, Color textFill, boolean dot) {
        HBox box = new HBox(14);
        box.setAlignment(Pos.BASELINE_LEFT);
        if (dot) box.getChildren().add(dot(7));
        box.getChildren().add(TrackedText.caps(text, ItiaFonts.eyebrow(),
                ItiaTokens.EYEBROW_TRACKING, textFill));
        return box;
    }

    /** Ponto vermelho da marca (7px na web, 8px nos slides). */
    public static Region dot(double size) {
        Region d = new Region();
        d.setPrefSize(size, size);
        d.setMinSize(size, size);
        d.setMaxSize(size, size);
        d.setStyle("-fx-background-color:" + ItiaTokens.rgba(ItiaTokens.ACCENT)
                + ";-fx-background-radius:" + (size / 2) + "px;");
        return d;
    }
}
