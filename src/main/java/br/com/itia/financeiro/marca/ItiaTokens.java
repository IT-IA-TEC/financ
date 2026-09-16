package br.com.itia.financeiro.marca;

import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Valores literais de tokens/colors.css, tokens/typography.css e tokens/spacing.css.
 * Nada aqui foi arredondado ou reinterpretado.
 */
public final class ItiaTokens {
    private ItiaTokens() {}

    // ---- Paleta base ----
    public static final Color BLACK    = Color.web("#111114"); // Ink
    public static final Color CHARCOAL = Color.web("#1C1C1F"); // Ink dois
    public static final Color GRAPHITE = Color.web("#3F3F46"); // Grafite
    public static final Color MIST     = Color.web("#A1A1AA"); // Nevoa
    public static final Color WHITE    = Color.web("#FFFFFF"); // Branco
    public static final Color RED      = Color.web("#FE4901"); // Laranja IT

    public static final Color MIST_40  = Color.web("#B9B9BE", 0.4);
    public static final Color WHITE_06 = Color.web("#FFFFFF", 0.06);
    public static final Color BLACK_90 = Color.web("#050506", 0.9);
    public static final Color BLACK_70 = Color.web("#050506", 0.7);  // scrim do Dialog
    public static final Color BLACK_06 = Color.web("#050506", 0.06); // hover IconButton
    public static final Color BLACK_04 = Color.web("#050506", 0.04); // hover ghost/secondary

    // ---- Semanticos ----
    public static final Color SURFACE_PAGE       = WHITE;
    public static final Color SURFACE_CARD       = WHITE;
    public static final Color SURFACE_INVERSE    = BLACK;
    public static final Color SURFACE_INVERSE_2  = CHARCOAL;
    public static final Color TEXT_PRIMARY       = BLACK;
    public static final Color TEXT_SECONDARY     = GRAPHITE;
    public static final Color TEXT_MUTED         = MIST;
    public static final Color TEXT_ON_INVERSE    = WHITE;
    public static final Color TEXT_ON_INV_MUTED  = MIST;
    public static final Color BORDER_DEFAULT     = MIST;
    public static final Color BORDER_ON_INVERSE  = GRAPHITE;
    public static final Color ACCENT             = RED;
    public static final Color ACCENT_FG          = WHITE;
    public static final Color ACCENT_HOVER       = Color.web("#D90000"); // hover do Button accent
    public static final Color LINK               = RED;
    public static final Color LINK_HOVER         = BLACK;

    // ---- Tipografia: tamanho / peso / tracking (em) / entrelinha ----
    public static final double DISPLAY_SIZE = 64, DISPLAY_TRACKING = -0.035, DISPLAY_LEADING = 1.0;
    public static final double H1_SIZE = 44, H1_TRACKING = -0.02,  H1_LEADING = 1.1;
    public static final double H2_SIZE = 28, H2_TRACKING = -0.01,  H2_LEADING = 1.2;
    public static final double H3_SIZE = 18, H3_LEADING = 1.3;
    public static final double LEAD_SIZE = 34, LEAD_LEADING = 1.35;
    public static final double BODY_SIZE = 17, BODY_LEADING = 1.6;
    public static final double SMALL_SIZE = 14, SMALL_LEADING = 1.5;
    public static final double CAPTION_SIZE = 12, CAPTION_TRACKING = 0.1;
    public static final double EYEBROW_SIZE = 13, EYEBROW_TRACKING = 0.2;
    public static final double META_SIZE = 11, META_TRACKING = 0.18;
    public static final double TAGLINE_TRACKING = 0.28;
    public static final double LOGO_TRACKING = -0.03;

    // ---- Espacamento ----
    public static final double SPACE_1 = 4, SPACE_2 = 8, SPACE_3 = 12, SPACE_4 = 16, SPACE_5 = 20,
            SPACE_6 = 24, SPACE_7 = 32, SPACE_8 = 40, SPACE_9 = 48, SPACE_10 = 56, SPACE_11 = 64,
            SPACE_12 = 80, SPACE_13 = 120;
    public static final double SECTION_PAD_Y = 120, SECTION_PAD_X = 40, CONTENT_MAX = 1160, SECTION_GAP = 56;
    public static final double GRID_MODULE = 64;

    // ---- Raios ----
    public static final double RADIUS_NONE = 0, RADIUS_SM = 6, RADIUS_MD = 12, RADIUS_LG = 20;
    /** border-radius: 22% — o JavaFX so aceita px, entao calculamos sobre a aresta. */
    public static double appIconRadius(double edge) { return edge * 0.22; }
    /** Logo.jsx variant="tile": Math.round(size * 0.17). */
    public static double tileRadius(double edge) { return Math.round(edge * 0.17); }

    // ---- Bordas e reguas ----
    public static final double BORDER_WIDTH = 1, RULE_ACCENT_WIDTH = 2, RULE_ACCENT_THICK = 3;

    // ---- Motion: cubic-bezier(.2,.7,.2,1) ----
    public static final Duration DURATION_FAST = Duration.millis(120);
    public static final Duration DURATION_BASE = Duration.millis(200);
    public static final javafx.animation.Interpolator EASE_OUT =
            javafx.animation.Interpolator.SPLINE(0.2, 0.7, 0.2, 1.0);

    public static String rgba(Color c) {
        return String.format("rgba(%d,%d,%d,%s)",
                (int) Math.round(c.getRed() * 255), (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255), c.getOpacity());
    }

    /** Anexa as folhas da marca: primeiro as cores, depois a tipografia. */
    public static void applyStylesheets(Scene scene) {
        scene.getStylesheets().addAll(
                ItiaTokens.class.getResource("itia-tokens.css").toExternalForm(),
                ItiaTokens.class.getResource("itia-typography.css").toExternalForm());
    }
}
