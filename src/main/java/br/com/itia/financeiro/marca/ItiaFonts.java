package br.com.itia.financeiro.marca;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.InputStream;

/**
 * Carga das fontes da marca. Nenhum binario veio no material original — coloque os .ttf em
 * src/main/resources/com/itia/ds/fonts/. O que faltar cai na familia instalada no sistema.
 */
public final class ItiaFonts {
    private ItiaFonts() {}

    public static final String DISPLAY = "Clash Display";
    public static final String BODY = "Geist";
    /** A identidade nova não tem letra de máquina: rótulo também é Geist. */
    public static final String MONO = "Geist";

    private static final String[] FILES = {
            "ClashDisplay-Medium.ttf", "ClashDisplay-Semibold.ttf", "ClashDisplay-Bold.ttf",
            "Geist-Regular.ttf", "Geist-Medium.ttf", "Geist-SemiBold.ttf", "Geist-Bold.ttf"
    };
    private static boolean loaded;

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        for (String file : FILES) {
            try (InputStream in = ItiaFonts.class.getResourceAsStream("fonts/" + file)) {
                if (in == null) {
                    System.err.println("[IT.IA] fonte ausente: " + file + " — usando a familia do sistema");
                    continue;
                }
                Font.loadFont(in, 12);
            } catch (Exception e) {
                System.err.println("[IT.IA] falha ao carregar " + file + ": " + e.getMessage());
            }
        }
    }

    /**
     * O Java trata cada espessura como uma família à parte: só "normal" e "negrito"
     * cabem na família base. Por isso o peso 500 e o 600 são pedidos pelo nome da
     * própria família, e não pelo número.
     */
    private static Font of(String family, FontWeight weight, double size) {
        if (BODY.equals(family)) {
            if (weight == FontWeight.MEDIUM) {
                return Font.font(family + " Medium", size);
            }
            if (weight == FontWeight.SEMI_BOLD) {
                return Font.font(family + " SemiBold", size);
            }
            return Font.font(family, weight, size);
        }
        // Clash Display: o 600 tem família própria e o 700 é o negrito da base.
        if (DISPLAY.equals(family)) {
            if (weight == FontWeight.MEDIUM) {
                return Font.font(family + " Medium", size);
            }
            if (weight == FontWeight.SEMI_BOLD) {
                return Font.font(family + " Semibold", size);
            }
            return Font.font(family, FontWeight.BOLD, size);
        }
        return Font.font(family, weight, size);
    }

    // Papeis tipograficos — tamanhos e pesos identicos a tokens/typography.css
    public static Font display()        { return of(DISPLAY, FontWeight.BOLD, ItiaTokens.DISPLAY_SIZE); }
    public static Font display(double s){ return of(DISPLAY, FontWeight.BOLD, s); }
    public static Font logotype(double s){ return of(DISPLAY, FontWeight.BOLD, s); }
    public static Font h1()             { return of(DISPLAY, FontWeight.SEMI_BOLD, ItiaTokens.H1_SIZE); }
    public static Font h2()             { return of(DISPLAY, FontWeight.SEMI_BOLD, ItiaTokens.H2_SIZE); }
    public static Font h3()             { return of(DISPLAY, FontWeight.SEMI_BOLD, ItiaTokens.H3_SIZE); }
    public static Font lead()           { return of(DISPLAY, FontWeight.SEMI_BOLD, ItiaTokens.LEAD_SIZE); }
    public static Font displaySemi(double s){ return of(DISPLAY, FontWeight.SEMI_BOLD, s); }
    public static Font body()           { return of(BODY, FontWeight.NORMAL, ItiaTokens.BODY_SIZE); }
    public static Font body(double s)   { return of(BODY, FontWeight.NORMAL, s); }
    public static Font bodySemi(double s){ return of(BODY, FontWeight.SEMI_BOLD, s); }
    public static Font bodyMedium(double s){ return of(BODY, FontWeight.MEDIUM, s); }
    public static Font small()          { return of(BODY, FontWeight.NORMAL, ItiaTokens.SMALL_SIZE); }
    public static Font caption()        { return of(BODY, FontWeight.MEDIUM, ItiaTokens.CAPTION_SIZE); }
    public static Font eyebrow()        { return of(BODY, FontWeight.SEMI_BOLD, ItiaTokens.EYEBROW_SIZE); }
    public static Font eyebrow(double s){ return of(BODY, FontWeight.SEMI_BOLD, s); }
    public static Font meta()           { return of(BODY, FontWeight.NORMAL, ItiaTokens.META_SIZE); }
    public static Font meta(double s)   { return of(BODY, FontWeight.NORMAL, s); }
    public static Font mono()           { return of(MONO, FontWeight.NORMAL, 13); }
    public static Font mono(double s)   { return of(MONO, FontWeight.NORMAL, s); }
}
