package de.bund.zrb.service;

import javax.swing.*;

public class DebugService {

    private static final DebugService INSTANCE = new DebugService();

    private JTextArea logArea;

    private DebugService() {
    }

    public static DebugService getInstance() {
        return INSTANCE;
    }

    public void setLogArea(JTextArea textArea) {
        this.logArea = textArea;
    }

    public void appendLog(String message) {
        if (logArea != null) {
            SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
        }
    }

    public void clearLog() {
        if (logArea != null) {
            SwingUtilities.invokeLater(() -> logArea.setText(""));
        }
    }
}
