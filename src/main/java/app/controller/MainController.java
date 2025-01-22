package app.controller;

import app.service.BrowserService;

import javax.swing.*;


public class MainController {
    private final BrowserService browserService;

    // ToDo: May use dependency injection via Spring or other frameworks
    public MainController()
    {
        this(new BrowserService());
    }

    public MainController(BrowserService browserService) {
        this.browserService = browserService;
    }

    public void onLaunchBrowser() {
        browserService.launchBrowser();
    }

    public void onCloseBrowser() {
        browserService.closeBrowser();
    }

    public void setupListeners(JButton navigateButton, JTextField addressBar, JComboBox<String> browserSelector) {
        navigateButton.addActionListener(e -> {
            String url = addressBar.getText();
            if (!url.isEmpty()) {
                browserService.navigateTo(url);
            } else {
                JOptionPane.showMessageDialog(null, "Bitte eine URL eingeben.");
            }
        });
    }
}
