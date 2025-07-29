package de.bund.zrb.ui.components.log;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import javax.swing.*;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
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
        PDDocumentOutline outline = new PDDocumentOutline();
        pdf.getDocumentCatalog().setDocumentOutline(outline);

        try {
            PDFStyle.initFont(pdf);

            final PDPage[] currentPage = {null};
            final PDPageContentStream[] content = {null};
            final float[] y = {PDRectangle.A4.getHeight() - 50};
            final float left = 50;
            final float lineHeight = 14;

            final PDOutlineItem[] currentSuiteItem = {null};

            for (LogComponent log : logComponents) {
                String html = log.toHtml();
                Reader reader = new StringReader(html);
                HTMLEditorKit.Parser parser = new HTMLKitWithPublicParser().getPublicParser();

                parser.parse(reader, new HTMLEditorKit.ParserCallback() {
                    final PDFStyle[] currentStyle = {PDFStyle.normal()};

                    private void ensurePage() throws IOException {
                        if (y[0] < 50 || content[0] == null) {
                            if (content[0] != null) content[0].close();
                            currentPage[0] = new PDPage(PDRectangle.A4);
                            pdf.addPage(currentPage[0]);
                            content[0] = new PDPageContentStream(pdf, currentPage[0]);
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
                    public void handleText(char[] data, int pos) {
                        try {
                            String line = new String(data).trim();
                            if (line.isEmpty()) return;

                            // 🔍 Strukturlogik aus Komponententyp:
                            if (log instanceof SuiteLog) {
                                PDFStyle style = PDFStyle.header(14);
                                flushLine(line, style);

                                PDPageFitDestination dest = new PDPageFitDestination();
                                dest.setPage(currentPage[0]);
                                PDOutlineItem tocItem = new PDOutlineItem();
                                tocItem.setTitle(line);
                                tocItem.setDestination(dest);

                                if (isTopLevelSuite(line)) {
                                    outline.addLast(tocItem);
                                    currentSuiteItem[0] = tocItem;
                                } else {
                                    if (currentSuiteItem[0] != null) {
                                        currentSuiteItem[0].addLast(tocItem);
                                    } else {
                                        outline.addLast(tocItem); // fallback
                                    }
                                }

                                tocItem.openNode();

                            } else if (log instanceof StepLog) {
                                flushLine(line, PDFStyle.normal());
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, true);
            }

            if (content[0] != null) content[0].close();

            outline.openNode();
            pdf.save(file);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Fehler beim PDF-Export: " + e.getMessage(),
                    "Exportfehler", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                pdf.close();
            } catch (IOException ignored) {}
        }
    }

    private boolean isTopLevelSuite(String name) {
        // z. B. "3" ist Top-Level, "3_1" oder "3.1" ist Sub-Suite
        return !name.contains("_") && !name.contains(".");
    }

    private String normalize(String text) {
        return text.replace("✅", "[OK]")
                .replace("🟢", "[Start]")
                .replace("⏹", "[Stop]")
                .replace("❌", "[Fehler]");
    }

    public void clear() {
        logComponents.clear();
        logPane.setText("<html><body></body></html>");
    }
}
