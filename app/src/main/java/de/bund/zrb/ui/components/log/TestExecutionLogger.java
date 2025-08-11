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
        float[] y = new float[]{PDRectangle.A4.getHeight() - 50};
        float left = 50;
        float bottom = 50;
        PDPageContentStream[] content = new PDPageContentStream[1];

        try {
            PDFStyle.initFont(pdf);
            pdf.addPage(page);
            content[0] = new PDPageContentStream(pdf, page);

            for (LogComponent root : logComponents) {
                if (root instanceof SuiteLog && root.getName() != null) {  // ✅ nur SuiteLogs mit Namen
                    PDOutlineItem suiteItem = createOutlineItem(root.getName(), page);
                    outline.addLast(suiteItem);
                    suiteItem.openNode();

                    renderComponent(pdf, content, root, suiteItem, page, 0, left, bottom, y);
                }
            }

            if (content[0] != null) content[0].close();
            outline.openNode();
            pdf.save(file);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Fehler beim PDF-Export: " + e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                pdf.close();
            } catch (IOException ignore) {}
        }
    }

    private void renderComponent(PDDocument pdf,
                                 PDPageContentStream[] content,
                                 LogComponent comp,
                                 PDOutlineItem parentToc,
                                 PDPage currentPage,
                                 int indent,
                                 float left,
                                 float bottom,
                                 float[] y) throws IOException {

        PDFStyle style = indent == 0 ? PDFStyle.header(16) : indent == 1 ? PDFStyle.header(13) : PDFStyle.normal();

        if (y[0] < bottom + style.size + style.spacing) {
            content[0].close();
            currentPage = new PDPage(PDRectangle.A4);
            pdf.addPage(currentPage);
            content[0] = new PDPageContentStream(pdf, currentPage);
            y[0] = PDRectangle.A4.getHeight() - 50;
        }

        if (comp instanceof StepLog) {
            StepLog step = (StepLog) comp;
            String line = step.getPhase() + ": " + step.getContent();
            y[0] = drawLine(content[0], normalize(line), PDFStyle.normal(), left + indent * 10, y[0]);
            return;
        }

        // Für SuiteLog oder TestCaseLog
        y[0] = drawLine(content[0], comp.getName(), style, left + indent * 10, y[0]);

        List<LogComponent> children = comp.getChildren();
        if (children != null && !children.isEmpty()) {
            for (int i = 0; i < children.size(); i++) {
                LogComponent child = children.get(i);

                PDOutlineItem childToc = null;
                if (!(child instanceof StepLog)) {
                    childToc = createOutlineItem(child.getName(), currentPage);
                    parentToc.addLast(childToc);
                    childToc.openNode();
                }

                renderComponent(pdf,
                        content,
                        child,
                        childToc != null ? childToc : parentToc,
                        currentPage,
                        indent + 1,
                        left,
                        bottom,
                        y);
            }
        }
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
