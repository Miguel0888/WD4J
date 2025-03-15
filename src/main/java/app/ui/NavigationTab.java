package app.ui;

import app.controller.MainController;

import javax.swing.*;

public class NavigationTab {
    private JToolBar toolbar;
    private JTextField addressBar;
    private JButton navigateButton, screenshotButton;

    public NavigationTab(MainController controller) {
        toolbar = new JToolBar();
        toolbar.setFloatable(false);

        addressBar = new JTextField("https://www.google.com", 30);
        navigateButton = new JButton("Navigate");
        screenshotButton = new JButton("\uD83D\uDCF8");

        toolbar.add(new JLabel("URL:"));
        toolbar.add(addressBar);
        toolbar.add(screenshotButton);
        toolbar.add(navigateButton);
    }

    public JToolBar getToolbar() {
        return toolbar;
    }
}
