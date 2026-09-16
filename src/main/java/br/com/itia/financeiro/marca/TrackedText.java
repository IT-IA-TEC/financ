package br.com.itia.financeiro.marca;

import javafx.geometry.VPos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.Locale;

/**
 * O JavaFX CSS nao tem letter-spacing. Esta classe reproduz o tracking da identidade
 * posicionando cada caractere com o mesmo avanco em em usado no CSS
 * (-0.035em display, -0.03em logotipo, 0.1em caption, 0.18em meta, 0.2em eyebrow, 0.28em tagline).
 */
public final class TrackedText {
    private static final Locale PT_BR = new Locale("pt", "BR");
    private TrackedText() {}

    public static Pane of(String text, Font font, double trackingEm, Color fill) {
        double tracking = trackingEm * font.getSize();
        Pane pane = new Pane();
        pane.setPickOnBounds(false);
        double x = 0, height = 0;
        for (int i = 0; i < text.length(); i++) {
            Text glyph = new Text(String.valueOf(text.charAt(i)));
            glyph.setFont(font);
            glyph.setFill(fill);
            glyph.setTextOrigin(VPos.TOP);
            glyph.setLayoutX(x);
            pane.getChildren().add(glyph);
            x += glyph.getLayoutBounds().getWidth() + tracking;
            height = Math.max(height, glyph.getLayoutBounds().getHeight());
        }
        double width = Math.max(0, x - tracking);
        pane.setPrefSize(width, height);
        pane.setMinSize(width, height);
        pane.setMaxSize(width, height);
        return pane;
    }

    /** text-transform: uppercase + tracking, como nos rotulos, eyebrows e metadados. */
    public static Pane caps(String text, Font font, double trackingEm, Color fill) {
        return of(text.toUpperCase(PT_BR), font, trackingEm, fill);
    }

    /** Duas cores num mesmo bloco trackeado — usado no wordmark (ponto vermelho). */
    public static Pane bicolor(String text, Font font, double trackingEm, Color fill,
                               String highlight, Color highlightFill) {
        double tracking = trackingEm * font.getSize();
        Pane pane = new Pane();
        pane.setPickOnBounds(false);
        double x = 0, height = 0;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            Text glyph = new Text(ch);
            glyph.setFont(font);
            glyph.setFill(highlight.contains(ch) ? highlightFill : fill);
            glyph.setTextOrigin(VPos.TOP);
            glyph.setLayoutX(x);
            pane.getChildren().add(glyph);
            x += glyph.getLayoutBounds().getWidth() + tracking;
            height = Math.max(height, glyph.getLayoutBounds().getHeight());
        }
        double width = Math.max(0, x - tracking);
        pane.setPrefSize(width, height);
        pane.setMinSize(width, height);
        pane.setMaxSize(width, height);
        return pane;
    }

    /** Régua sólida: width x height em px, sem raio. */
    public static Region rule(double width, double height, Color color) {
        Region r = new Region();
        r.setPrefSize(width, height);
        r.setMinSize(width, height);
        r.setMaxSize(width, height);
        r.setStyle("-fx-background-color:" + ItiaTokens.rgba(color) + ";-fx-background-radius:0;");
        return r;
    }
}
