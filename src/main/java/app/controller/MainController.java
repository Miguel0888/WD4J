package app.controller;

import app.service.BrowserService;

import javax.swing.*;

public class MainController {
    private final BrowserService browserService;

    // Konstruktor
    public MainController() {
        this(new BrowserService());
    }

    public MainController(BrowserService browserService) {
        this.browserService = browserService;
    }

    // Browser schließen
    public void onCloseBrowser() {
        browserService.terminateWebDriver();
    }

    // Listener-Setup
    public void setupListeners(
            JTextField portField,
            JTextField profilePathField,
            JCheckBox headlessCheckbox,
            JCheckBox disableGpuCheckbox,
            JCheckBox noRemoteCheckbox,
            JComboBox<String> browserSelector,
            JButton launchButton,
            JButton terminateButton,
            JButton navigateButton,
            JTextField addressBar
    ) {
        // Browser starten
        launchButton.addActionListener(e -> {
            String selectedBrowser = (String) browserSelector.getSelectedItem();
            String portText = portField.getText();
            String profilePath = null;
            if( profilePathField.isEnabled())
            {
                profilePath = profilePathField.getText();
            }
            boolean headless = headlessCheckbox.isSelected();
            boolean disableGpu = disableGpuCheckbox.isSelected();
            boolean noRemote = noRemoteCheckbox.isSelected();

            if (selectedBrowser != null && !portText.isEmpty()) {
                try {
                    int port = Integer.parseInt(portText); // Portnummer verarbeiten
                    browserService.createWebDriver(
                            selectedBrowser,   // Browser-Typ (z. B. "Chrome")
                            port,              // Port
                            profilePath,       // Profilpfad
                            headless,          // Headless-Modus
                            disableGpu,        // GPU deaktivieren
                            noRemote           // No Remote
                    );
                    JOptionPane.showMessageDialog(null, selectedBrowser + " erfolgreich gestartet.");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Bitte eine gültige Portnummer eingeben.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Fehler beim Starten des Browsers: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(null, "Bitte einen Browser und einen gültigen Port auswählen.");
            }
        });

        // Browser beenden
        terminateButton.addActionListener(e -> {
            browserService.terminateWebDriver();
            JOptionPane.showMessageDialog(null, "Browser wurde beendet.");
        });

        // URL navigieren
        navigateButton.addActionListener(e -> {
            String url = addressBar.getText();
            if (!url.isEmpty()) {
                try {
                    browserService.navigateTo(url);
                    JOptionPane.showMessageDialog(null, "Navigiere zu: " + url);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Fehler beim Navigieren: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(null, "Bitte eine gültige URL eingeben.");
            }
        });
    }
}
