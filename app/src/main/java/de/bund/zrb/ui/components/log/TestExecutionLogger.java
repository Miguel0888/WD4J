package de.bund.zrb.ui.components.log;

import de.bund.zrb.ui.components.log.LogComponent;
import de.bund.zrb.ui.components.log.PDFStyle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
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

            PDPage page = new PDPage(PDRectangle.A4);
            pdf.addPage(page);
            PDPageContentStream content = new PDPageContentStream(pdf, page);
            float[] y = {PDRectangle.A4.getHeight() - 50};

            renderRootComponents(logComponents, outline, pdf, page, content, y);

            content.close();
            outline.openNode();
            pdf.save(file);

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "PDF-Export fehlgeschlagen: " + e.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                pdf.close();
            } catch (IOException ignored) {}
        }
    }

    private void renderRootComponents(List<LogComponent> components,
                                      PDDocumentOutline outline,
                                      PDDocument pdf,
                                      PDPage page,
                                      PDPageContentStream content,
                                      float[] y) throws IOException {
        for (LogComponent comp : components) {
            PDOutlineItem item = createOutlineItem(comp.getName(), page);
            outline.addLast(item);
            item.openNode();
            renderComponents(comp, item, pdf, page, content, 0, y);
        }
    }

    private void renderComponents(LogComponent comp,
                                  PDOutlineItem parentItem,
                                  PDDocument pdf,
                                  PDPage currentPage,
                                  PDPageContentStream content,
                                  int indentLevel,
                                  float[] y) throws IOException {
        float left = 50;
        float bottomMargin = 50;

        if (y[0] < bottomMargin) {
            content.close();
            currentPage = new PDPage(PDRectangle.A4);
            pdf.addPage(currentPage);
            content = new PDPageContentStream(pdf, currentPage);
            y[0] = PDRectangle.A4.getHeight() - 50;
        }

        PDFStyle style = indentLevel == 0
                ? PDFStyle.header(16)
                : indentLevel == 1 ? PDFStyle.header(13)
                : PDFStyle.normal();

        // 🔍 StepLog: Kein TOC-Eintrag – nur Text rendern
        if (comp.getChildren().isEmpty()) {
            y[0] = drawText(content, comp.toHtml(), style, left + indentLevel * 10, y[0]);
            y[0] -= 4;
            return;
        }

        // Alle anderen mit Name + TOC
        y[0] = drawText(content, comp.getName(), style, left + indentLevel * 10, y[0]);
        y[0] -= 4;

        for (LogComponent child : comp.getChildren()) {
            PDOutlineItem childItem = null;
            if (child.getChildren().size() > 0 && child.getName() != null && !child.getName().trim().isEmpty()) {
                childItem = createOutlineItem(child.getName(), currentPage);
                parentItem.addLast(childItem);
                childItem.openNode();
            }

            renderComponents(child,
                    childItem != null ? childItem : parentItem,
                    pdf, currentPage, content,
                    indentLevel + 1, y);
        }
    }


    private float drawText(PDPageContentStream content, String text, PDFStyle style, float left, float y) throws IOException {
        content.beginText();
        content.setFont(style.font, style.size);
        content.newLineAtOffset(left + style.indent, y);
        content.showText(stripHtml(normalize(text)));
        content.endText();
        return y - style.spacing - style.size;
    }

    private PDOutlineItem createOutlineItem(String title, PDPage page) {
        PDPageFitDestination dest = new PDPageFitDestination();
        dest.setPage(page);
        PDOutlineItem item = new PDOutlineItem();
        item.setDestination(dest);
        item.setTitle(title);
        return item;
    }

    private String normalize(String text) {
        return text.replace("✅", "[OK]")
                .replace("❌", "[Fehler]")
                .replace("🟢", "[Start]")
                .replace("⏹", "[Stop]");
    }

    private String stripHtml(String html) {
        return html.replaceAll("(?s)<[^>]*>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .trim();
    }

    public void clear() {
        logComponents.clear();
        logPane.setText("<html><body></body></html>");
    }
}
