package de.bund.zrb.ui.components.log;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TestExecutionLogger {

    private final JEditorPane logPane;
    private final List<LogComponent> logComponents = new ArrayList<>();

    public TestExecutionLogger(JEditorPane logPane) {
        this.logPane = logPane;
        this.logPane.setContentType("text/html");
        this.logPane.setText("<html><body></body></html>");
    }

    public void append(LogComponent log) {
        logComponents.add(log);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String html = log.toHtml();
                String current = logPane.getText();
                int insertPos = current.lastIndexOf("</body>");
                String updated = current.substring(0, insertPos) + html + current.substring(insertPos);
                logPane.setText(updated);
                logPane.setCaretPosition(logPane.getDocument().getLength());
            }
        });
    }

    public void exportAsPdf(File file) {
        PDDocument pdf = new PDDocument();
        PDDocumentOutline outline = new PDDocumentOutline();
        pdf.getDocumentCatalog().setDocumentOutline(outline);

        PDPage page = new PDPage(PDRectangle.A4);
        float pageWidth  = PDRectangle.A4.getWidth();
        float pageHeight = PDRectangle.A4.getHeight();

        final float left   = 50f;
        final float right  = 50f;
        final float top    = 50f;
        final float bottom = 50f;
        final float contentWidth = pageWidth - left - right;

        // y[0] als “mutable float”
        final float[] y = new float[]{ pageHeight - top };
        final PDPageContentStream[] content = new PDPageContentStream[1];

        try {
            PDFStyle.initFont(pdf);
            pdf.addPage(page);
            content[0] = new PDPageContentStream(pdf, page);

            for (LogComponent root : logComponents) {
                // Nur Suite-ähnliche Top-Level-Einträge ins Outline aufnehmen
                PDOutlineItem suiteItem = null;
                if (!(root instanceof StepLog) && root.getName() != null) {
                    suiteItem = createOutlineItem(root.getName(), page);
                    outline.addLast(suiteItem);
                    suiteItem.openNode();
                }

                renderComponentRecursive(
                        pdf, content,
                        root,
                        (suiteItem != null ? suiteItem : null),
                        new PageBox(page, pageWidth, pageHeight, contentWidth, left, right, top, bottom),
                        y,
                        0 // indent level
                );
            }

            if (content[0] != null) content[0].close();
            outline.openNode();
            pdf.save(file);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Fehler beim PDF-Export: " + e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        } finally {
            try { pdf.close(); } catch (IOException ignore) {}
        }
    }

    /** Carry page metrics + margins around cleanly. */
    private static final class PageBox {
        final PDPage page;
        final float pageWidth;
        final float pageHeight;
        final float contentWidth;
        final float left;
        final float right;
        final float top;
        final float bottom;

        PageBox(PDPage page, float pageWidth, float pageHeight, float contentWidth,
                float left, float right, float top, float bottom) {
            this.page = page;
            this.pageWidth = pageWidth;
            this.pageHeight = pageHeight;
            this.contentWidth = contentWidth;
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
    }

    private void renderComponentRecursive(PDDocument pdf,
                                          PDPageContentStream[] content,
                                          LogComponent comp,
                                          PDOutlineItem currentToc,
                                          PageBox pg,
                                          float[] y,
                                          int indent) throws IOException {

        final PDFStyle style = (indent == 0) ? PDFStyle.header(16)
                : (indent == 1) ? PDFStyle.header(13)
                : PDFStyle.normal();

        // Falls nicht genug Platz bleibt: neue Seite
        ensureSpaceOrNewPage(pdf, content, pg, y, style.size + style.spacing);

        // Suite/TestCase-Titel oder Step-Zeile
        if (comp instanceof StepLog) {
            StepLog step = (StepLog) comp;

            // 1) Zeile bauen (Phase + Content) und umbrechen
            String line = step.getPhase() + ": " + normalize(step.getContent());
            y[0] = drawWrapped(content[0], line, PDFStyle.normal(),
                    pg.left + indent * 10f, y[0], pg.contentWidth - indent * 10f);

            // 2) Optional: HTML-Anhang rendern (z. B. <img src="...">)
            String htmlAppend = step.getHtmlAppend();
            if (htmlAppend != null && htmlAppend.indexOf("<img") >= 0) {
                java.util.List<String> imgs = extractImgSrcs(htmlAppend);
                for (int i = 0; i < imgs.size(); i++) {
                    y[0] = drawImageInline(pdf, content, pg, y, imgs.get(i), indent);
                }
            }

            return;
        } else {
            // Überschrift für Container (Suite/Case/…)
            String title = (comp.getName() != null) ? comp.getName() : "";
            y[0] = drawWrapped(content[0], normalize(title), style,
                    pg.left + indent * 10f, y[0], pg.contentWidth - indent * 10f);
        }

        // Children
        List<LogComponent> children = comp.getChildren();
        if (children == null || children.isEmpty()) return;

        for (int i = 0; i < children.size(); i++) {
            LogComponent child = children.get(i);

            PDOutlineItem childToc = null;
            if (!(child instanceof StepLog) && child.getName() != null) {
                childToc = createOutlineItem(child.getName(), pg.page);
                if (currentToc != null) currentToc.addLast(childToc);
                if (childToc != null) childToc.openNode();
            }

            renderComponentRecursive(pdf, content, child,
                    (childToc != null ? childToc : currentToc),
                    pg, y, indent + 1);
        }
    }

    private void ensureSpaceOrNewPage(PDDocument pdf,
                                      PDPageContentStream[] content,
                                      PageBox pg,
                                      float[] y,
                                      float needed) throws IOException {
        if (y[0] < pg.bottom + needed) {
            // Seite schließen und neu beginnen
            if (content[0] != null) content[0].close();
            PDPage newPage = new PDPage(PDRectangle.A4);
            pdf.addPage(newPage);
            content[0] = new PDPageContentStream(pdf, newPage);
            y[0] = pg.pageHeight - pg.top;
            // Auch PageBox aktualisieren
            pg.page.getCOSObject().setNeedToBeUpdated(true); // harmless
            // neues Page-Objekt rein
            // (Für unser Rendering reicht, wenn wir y und content erneuern.)
        }
    }

    /** Zeichnet einen Text mit einfachem Wortumbruch über die verfügbare Breite. */
    private float drawWrapped(PDPageContentStream content,
                              String text,
                              PDFStyle style,
                              float x,
                              float y,
                              float maxWidth) throws IOException {

        if (text == null) text = "";
        content.setFont(style.font, style.size);

        java.util.List<String> lines = wrap(style.font, style.size, text, maxWidth);
        for (int i = 0; i < lines.size(); i++) {
            String ln = lines.get(i);
            content.beginText();
            content.newLineAtOffset(x + style.indent, y);
            content.showText(ln);
            content.endText();
            y -= (style.size + style.spacing);
        }
        return y;
    }

    /** Primitive Wortwrap-Logik basierend auf PDFont.stringWidth. */
    private java.util.List<String> wrap(org.apache.pdfbox.pdmodel.font.PDFont font,
                                        float fontSize,
                                        String text,
                                        float maxWidth) throws IOException {
        java.util.List<String> out = new java.util.ArrayList<String>();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            String cand = (line.length() == 0) ? w : (line.toString() + " " + w);
            float wWidth = font.getStringWidth(cand) / 1000f * fontSize;
            if (wWidth <= maxWidth) {
                line.setLength(0);
                line.append(cand);
            } else {
                if (line.length() > 0) {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(w);
                } else {
                    // Wort länger als Zeile – brutaler Cut
                    out.add(w);
                }
            }
        }
        if (line.length() > 0) out.add(line.toString());
        if (out.isEmpty()) out.add(""); // nie leer zurückgeben
        return out;
    }

    /** Extrahiert alle src-Werte aus <img ...> Tags im gegebenen HTML-Schnipsel. */
    private java.util.List<String> extractImgSrcs(String html) {
        java.util.List<String> list = new java.util.ArrayList<String>();
        if (html == null) return list;
        // sehr einfache Extraktion
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<img[^>]*?src=['\"]([^'\"]+)['\"][^>]*>", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(html);
        while (m.find()) {
            list.add(m.group(1));
        }
        return list;
    }

    /** Zeichnet ein Bild (relativ oder absolut) skaliert auf Seitenbreite. */
    private float drawImageInline(PDDocument pdf,
                                  PDPageContentStream[] content,
                                  PageBox pg,
                                  float[] y,
                                  String src,
                                  int indent) throws IOException {
        if (src == null || src.trim().length() == 0) return y[0];

        // Basisverzeichnis aus JEditorPane-Dokument (wurde über setDocumentBase gesetzt)
        java.net.URL base = null;
        try {
            javax.swing.text.Document doc = logPane.getDocument();
            if (doc instanceof javax.swing.text.html.HTMLDocument) {
                base = ((javax.swing.text.html.HTMLDocument) doc).getBase();
            }
        } catch (Throwable ignore) {}

        java.net.URL url;
        try {
            if (base != null) {
                url = new java.net.URL(base, src);
            } else {
                // Fallback: direkt interpretieren
                url = new java.net.URL(src);
            }
        } catch (Exception ex) {
            // vielleicht ist es ein Dateipfad
            File f = new File(src);
            url = f.toURI().toURL();
        }

        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(url);
        if (img == null) return y[0];

        org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImg =
                org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(pdf, img);

        // Skalierung: maximal contentWidth - indent
        float maxW = pg.contentWidth - indent * 10f;
        float scale = 1f;
        if (pdImg.getWidth() > maxW) {
            scale = maxW / pdImg.getWidth();
        }
        float drawW = pdImg.getWidth() * scale;
        float drawH = pdImg.getHeight() * scale;

        // Platz prüfen
        ensureSpaceOrNewPage(pdf, content, pg, y, drawH + 6f);

        float drawX = pg.left + indent * 10f;
        float drawY = y[0] - drawH;

        content[0].drawImage(pdImg, drawX, drawY, drawW, drawH);
        y[0] = drawY - 6f; // kleiner Abstand nach unten
        return y[0];
    }

    private float drawLine(PDPageContentStream content,
                           String text,
                           PDFStyle style,
                           float x,
                           float y) throws IOException {
        content.beginText();
        content.setFont(style.font, style.size);
        content.newLineAtOffset(x + style.indent, y);
        content.showText(text);
        content.endText();
        return y - style.spacing - style.size;
    }

    private PDOutlineItem createOutlineItem(String title, PDPage page) {
        PDPageFitDestination dest = new PDPageFitDestination();
        dest.setPage(page);
        PDOutlineItem item = new PDOutlineItem();
        item.setTitle(title);
        item.setDestination(dest);
        return item;
    }

    private String normalize(String text) {
        return text.replace("✅", "[OK]")
                .replace("❌", "[Fehler]")
                .replace("🟢", "[Start]")
                .replace("⏹", "[Stop]");
    }

    public void clear() {
        logComponents.clear();
        logPane.setText("<html><body></body></html>");
    }

    /** NEU: Legt die Base-URL fest, damit <img src="relativ.png"> im JEditorPane sofort angezeigt wird. */
    public void setDocumentBase(Path baseDir) {
        try {
            Document doc = logPane.getDocument();
            if (doc instanceof HTMLDocument) {
                ((HTMLDocument) doc).setBase(baseDir.toUri().toURL());
            }
        } catch (Exception ignore) {
            // Base ist "nice to have" – kein harter Fehler
        }
    }

    public void exportAsHtml(Path htmlFile) {
        try {
            Files.createDirectories(htmlFile.getParent());
            String html = buildHtmlFromComponents();
            Files.write(htmlFile, html.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String buildHtmlFromComponents() {
        StringBuilder body = new StringBuilder();
        for (LogComponent root : logComponents) {
            body.append(root.toHtml()).append("\n"); // SuiteLog sollte rekursiv Children rendern
        }
        return "<!doctype html><html><head><meta charset='utf-8'>"
                + "<title>Test Report</title>"
                + "<style>body{font-family:Segoe UI,Arial,sans-serif;line-height:1.35}"
                + "img{max-width:100%;border:1px solid #ccc;margin-top:.5rem}</style>"
                + "</head><body>\n" + body + "\n</body></html>";
    }
}
