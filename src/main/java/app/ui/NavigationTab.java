package app.ui;

import app.controller.MainController;

import javax.swing.*;

public class NavigationTab {
    private final MainController controller;
    private JToolBar toolbar;
    private JTextField addressBar;

    public NavigationTab(MainController controller) {
        this.controller = controller;
        toolbar = createNavigationToolBar();
    }

    private JToolBar createNavigationToolBar()
    {
        JToolBar navigationToolBar = new JToolBar();
        navigationToolBar.setFloatable(false);

        JButton goBackButton = new JButton("\u21A9");
        goBackButton.setToolTipText("Back");
        JButton goForwardButton = new JButton("\u21AA");
        goForwardButton.setToolTipText("Forward");
        JButton reloadButton = new JButton("\uD83D\uDD04");
        reloadButton.setToolTipText("Reload");

        addressBar = new JTextField("https://www.google.com", 30);
        JButton navigateButton = new JButton("Navigate");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Event Listeners
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        navigateButton.addActionListener(controller::onNavigate);
        goBackButton.addActionListener(e -> controller.goBack());
        goForwardButton.addActionListener(e -> controller.goForward());
        reloadButton.addActionListener(e -> controller.reload());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Toolbar
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        navigationToolBar.add(goBackButton);
        navigationToolBar.add(goForwardButton);
        navigationToolBar.add(reloadButton);
        navigationToolBar.add(new JLabel("URL:"));
        navigationToolBar.add(addressBar);
        navigationToolBar.add(navigateButton);
        return navigationToolBar;
    }

    public JToolBar getToolbar() {
        return toolbar;
    }

    public JTextField getAddressBar() {
        return addressBar;
    }
}
