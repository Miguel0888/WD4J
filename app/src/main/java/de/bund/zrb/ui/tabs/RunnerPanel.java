package de.bund.zrb.ui.tabs;

import javax.swing.*;
import java.awt.*;

public class RunnerPanel extends JPanel {

    private final JTextArea logArea;

    public RunnerPanel() {
        super(new BorderLayout());

        JLabel title = new JLabel("Test Runner");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        add(title, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void appendLog(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
