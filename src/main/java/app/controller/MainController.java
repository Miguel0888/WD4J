package app.controller;

import app.service.BrowserService;
import wd4j.impl.BrowserType;

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

    public void setupListeners(JButton launchButton, JComboBox<String> browserSelector, JButton navigateButton, JTextField addressBar) {
        // Browser starten
        launchButton.addActionListener(e -> {
            String selectedBrowser = (String) browserSelector.getSelectedItem();
            if (selectedBrowser != null) {
                try {
                    // Browser basierend auf der Auswahl starten
                    browserService.launchBrowser(BrowserType.valueOf(selectedBrowser.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(null, "Der ausgewählte Browser wird nicht unterstützt.");
                }
            }
        });

        // Navigieren
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
