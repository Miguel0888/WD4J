package app.ui;

import app.controller.MainController;

import javax.swing.*;

public class DebugTab {
    private JToolBar toolbar;
    private JButton clearConsoleButton;

    public DebugTab(MainController controller) {
        toolbar = new JToolBar();
        toolbar.setFloatable(false);

        clearConsoleButton = new JButton("Clear Console");
        clearConsoleButton.addActionListener(e -> controller.clearLog());

        toolbar.add(clearConsoleButton);
    }

    public JToolBar getToolbar() {
        return toolbar;
    }
}
