package app.ui;

import javax.swing.*;

public class ConsoleTab {
    private JPanel panel;
    private JTextArea console;

    public ConsoleTab() {
        console = new JTextArea();
        console.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(console);
        panel = new JPanel();
        panel.setLayout(new java.awt.BorderLayout());
        panel.add(scrollPane, java.awt.BorderLayout.CENTER);
    }

    public JPanel getPanel() {
        return panel;
    }

    public JTextArea getConsole() {
        return console;
    }
}
