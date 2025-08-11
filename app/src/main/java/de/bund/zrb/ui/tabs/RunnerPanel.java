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
        this.logPane.setContentType("text/html");
        this.logPane.setText(
                "<html><head><style>"
                        + "body{font-family:sans-serif;font-size:12px;margin:6px;}"
                        + "p{margin:4px 0;}"                   // schlanke Abstände
                        + "img{max-width:100%;}"               // falls Screenshots eingebettet sind
                        + "</style></head><body></body></html>"
        );

        JScrollPane scrollPane = new JScrollPane(logPane);
        add(scrollPane, BorderLayout.CENTER);

        logger = new TestExecutionLogger(logPane);

        // Titelleiste mit rechtsbündigem Download-Symbol
        JPanel header = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Test Runner");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

        JButton downloadButton = new JButton("⭳"); // Unicode für Pfeil nach untenMC
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

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
