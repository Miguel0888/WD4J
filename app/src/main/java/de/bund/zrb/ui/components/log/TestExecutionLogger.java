package de.bund.zrb.ui.components.log;

import javax.swing.*;

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

}
