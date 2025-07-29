package de.bund.zrb.ui.components.log;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class TestExecutionLogger {

    private final JEditorPane logPane;

    public TestExecutionLogger(JEditorPane logPane) {
        this.logPane = logPane;
        this.logPane.setContentType("text/html");
        this.logPane.setText("<html><body></body></html>");
    }

    public void append(LogComponent log) {
        SwingUtilities.invokeLater(() -> {
            String html = log.toHtml();
            String current = logPane.getText();
            int insertPos = current.lastIndexOf("</body>");
            String updated = current.substring(0, insertPos) + html + "<br>" + current.substring(insertPos);
            logPane.setText(updated);
            logPane.setCaretPosition(logPane.getDocument().getLength());
        });
    }

    public void exportAsPdf(File file) {
        PDDocument pdf = new PDDocument();
        try {
            // 🧠 Initialisiere den Font einmal VOR dem Parser
            PDFStyle.initFont(pdf);

            String html = logPane.getText();
            Reader reader = new StringReader(html);

            HTMLKitWithPublicParser kit = new HTMLKitWithPublicParser();
            HTMLEditorKit.Parser parser = kit.getPublicParser();

            final PDPageContentStream[] content = {null};
            final float[] y = {PDRectangle.A4.getHeight() - 50};
            final float left = 50;
            final float lineHeight = 14;

            parser.parse(reader, new HTMLEditorKit.ParserCallback() {
                final PDFStyle[] currentStyle = {PDFStyle.normal()};

                private void ensurePage() throws IOException {
                    if (y[0] < 50 || content[0] == null) {
                        if (content[0] != null) content[0].close();
                        PDPage page = new PDPage(PDRectangle.A4);
                        pdf.addPage(page);
                        content[0] = new PDPageContentStream(pdf, page);
                        y[0] = PDRectangle.A4.getHeight() - 50;
                    }
                }

                private void flushLine(String text, PDFStyle style) throws IOException {
                    ensurePage();
                    content[0].beginText();
                    content[0].setFont(style.font, style.size);
                    content[0].newLineAtOffset(left + style.indent, y[0]);
                    content[0].showText(normalize(text));
                    content[0].endText();
                    y[0] -= lineHeight + style.spacing;
                }

                @Override
                public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int pos) {
                    if (tag == HTML.Tag.H1) currentStyle[0] = PDFStyle.header(16);
                    else if (tag == HTML.Tag.H2) currentStyle[0] = PDFStyle.header(14);
                    else if (tag == HTML.Tag.UL) currentStyle[0] = PDFStyle.listItem();
                    else if (tag == HTML.Tag.P) currentStyle[0] = PDFStyle.normal();
                }

                @Override
                public void handleText(char[] data, int pos) {
                    try {
                        String line = new String(data).trim();
                        if (!line.isEmpty()) {
                            flushLine(line, currentStyle[0]);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet a, int pos) {
                    if (tag == HTML.Tag.BR) {
                        y[0] -= lineHeight;
                    }
                }

                @Override
                public void handleEndTag(HTML.Tag tag, int pos) {
                    currentStyle[0] = PDFStyle.normal();
                }

            }, true);

            if (content[0] != null) content[0].close();
            pdf.save(file);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Fehler beim PDF-Export: " + e.getMessage(),
                    "Exportfehler", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            try {
                pdf.close();
            } catch (IOException ignored) {}
        }
    }

    private String normalize(String text) {
        return text
                .replace("✅", "[OK]")
                .replace("🟢", "[Start]")
                .replace("⏹", "[Stop]")
                .replace("❌", "[Fehler]");
    }

    private String stripHtmlTags(String html) {
        return html.replaceAll("(?s)<[^>]*>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .trim();
    }
}
