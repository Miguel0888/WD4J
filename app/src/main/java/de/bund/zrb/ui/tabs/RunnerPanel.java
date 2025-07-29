package de.bund.zrb.ui.tabs;

import de.bund.zrb.ui.components.log.TestExecutionLogger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RunnerPanel extends JPanel {

    private final JEditorPane logPane;
    private final TestExecutionLogger logger;

    public RunnerPanel() {
        super(new BorderLayout());

        // Logbereich
        logPane = new JEditorPane();
        logPane.setEditable(false);
        logPane.setContentType("text/html");
        logPane.setText("<html><body></body></html>");

        JScrollPane scrollPane = new JScrollPane(logPane);
        add(scrollPane, BorderLayout.CENTER);

        logger = new TestExecutionLogger(logPane);

        // Titelleiste mit rechtsbündigem Download-Symbol
        JPanel header = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Test Runner");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

        JButton downloadButton = new JButton("⭳"); // Unicode für Pfeil nach unten
        downloadButton.setToolTipText("Log als PDF exportieren");
        downloadButton.setFocusPainted(false);
        downloadButton.setMargin(new Insets(2, 8, 2, 8));

        downloadButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            chooser.setSelectedFile(new File("test-report-" + date + ".pdf"));
            int result = chooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                logger.exportAsPdf(chooser.getSelectedFile());
            }
        });

        header.add(title, BorderLayout.WEST);
        header.add(downloadButton, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
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
