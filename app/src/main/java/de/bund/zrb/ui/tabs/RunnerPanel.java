package de.bund.zrb.ui.tabs;

import de.bund.zrb.ui.components.log.TestExecutionLogger;

import javax.swing.*;
import java.awt.*;

public class RunnerPanel extends JPanel {

    private final JEditorPane logPane;
    private final TestExecutionLogger logger;

    public RunnerPanel() {
        super(new BorderLayout());

        JLabel title = new JLabel("Test Runner");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        add(title, BorderLayout.NORTH);

        logPane = new JEditorPane();
        logPane.setEditable(false);
        logPane.setContentType("text/html");
        logPane.setText("<html><body></body></html>");

        JScrollPane scrollPane = new JScrollPane(logPane);
        add(scrollPane, BorderLayout.CENTER);

        logger = new TestExecutionLogger(logPane);
    }

    /**
     * Zugriff für externe Services (z. B. TestPlayerService)
     */
    public TestExecutionLogger getLogger() {
        return logger;
    }

    /**
     * Optional: Plaintext-Log als HTML-Zeile einfügen
     */
    public void appendLog(String message) {
        logger.append(() -> "<p>" + escapeHtml(message) + "</p>");
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
