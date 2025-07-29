package de.bund.zrb.ui.components.log;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;

public class PDFStyle {
    public final PDFont font;
    public final float size;
    public final float indent;
    public final float spacing;

    private static PDFont defaultFont;

    private PDFStyle(PDFont font, float size, float indent, float spacing) {
        this.font = font;
        this.size = size;
        this.indent = indent;
        this.spacing = spacing;
    }

    public static void initFont(PDDocument doc) throws IOException {
        // Versuche, unter Windows Segoe UI zu laden – Pfad ggf. anpassen
        File fontFile = new File(System.getenv("WINDIR") + "\\Fonts\\segoeui.ttf");
        if (!fontFile.exists()) {
            throw new IOException("Font 'Segoe UI' nicht gefunden: " + fontFile.getAbsolutePath());
        }
        defaultFont = PDType0Font.load(doc, fontFile);
    }

    public static PDFStyle normal() {
        ensureFontLoaded();
        return new PDFStyle(defaultFont, 10, 0, 2);
    }

    public static PDFStyle header(float size) {
        ensureFontLoaded();
        return new PDFStyle(defaultFont, size, 0, 8);
    }

    public static PDFStyle listItem() {
        ensureFontLoaded();
        return new PDFStyle(defaultFont, 10, 10, 2);
    }

    public static PDFStyle normal(PDFont font) {
        return new PDFStyle(font, 10, 0, 2);
    }

    public static PDFStyle header(PDFont font, float size) {
        return new PDFStyle(font, size, 0, 8);
    }

    public static PDFStyle listItem(PDFont font) {
        return new PDFStyle(font, 10, 10, 2);
    }

    private static void ensureFontLoaded() {
        if (defaultFont == null) {
            throw new IllegalStateException("PDFStyle: call PDFStyle.initFont(doc) before using default styles");
        }
    }
}
