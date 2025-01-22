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

    public void setupListeners(JTextField portField, JComboBox<String> browserSelector, JButton launchButton, JButton navigateButton, JTextField addressBar) {
        // Browser starten
        launchButton.addActionListener(e -> {
            String selectedBrowser = (String) browserSelector.getSelectedItem();
            String portText = portField.getText();

            if (selectedBrowser != null && !portText.isEmpty()) {
                try {
                    int port = Integer.parseInt(portText); // Port in Integer umwandeln
                    browserService.launchBrowser(BrowserType.valueOf(selectedBrowser.toUpperCase()), port);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Bitte eine gültige Portnummer eingeben.");
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(null, "Der ausgewählte Browser wird nicht unterstützt.");
                }
            } else {
                JOptionPane.showMessageDialog(null, "Bitte einen Browser und einen gültigen Port auswählen.");
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
