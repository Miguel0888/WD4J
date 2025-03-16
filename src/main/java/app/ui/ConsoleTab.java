package app.ui;

import app.controller.MainController;

import javax.swing.*;
import java.awt.*;

public class ConsoleTab {
    private JPanel panel;
    private JTextArea console;
    private JButton clearConsoleButton;

    public ConsoleTab(MainController controller) {
        panel = new JPanel(new BorderLayout());

        console = new JTextArea();
        console.setEditable(false);
        JScrollPane consoleScrollPane = new JScrollPane(console);

        clearConsoleButton = new JButton("Clear Console");
        clearConsoleButton.addActionListener(e -> console.setText(""));

        panel.add(consoleScrollPane, BorderLayout.CENTER);
    }

    public JPanel getPanel() {
        return panel;
    }

    public JTextArea getConsole() {
        return console;
    }

    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> console.append(message + "\n"));
    }

    public void clearLog() {
        SwingUtilities.invokeLater(() -> console.setText(""));
    }
}
