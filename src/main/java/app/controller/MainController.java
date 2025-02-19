package app.controller;

import wd4j.api.*;

import javax.swing.*;
import java.util.*;

public class MainController {
    private Playwright playwright;
    private Browser browser;
    private BrowserContext browserContext;
    private Page page;

    private JTextArea eventLog;
    private boolean loggingActive = false; // Status für Play/Pause

    // Browser schließen
    public void onCloseBrowser() {
        if (browser != null) {
            browser.close();
            playwright.close();
        }
    }

    // Listener-Setup
    public void setupListeners(
            JTextField portField,
            JTextField profilePathField,
            JCheckBox headlessCheckbox,
            JCheckBox disableGpuCheckbox,
            JCheckBox noRemoteCheckbox,
            JCheckBox startMaximizedCheckbox,
            JComboBox<String> browserSelector,
            JButton launchButton,
            JButton terminateButton,
            JButton navigateButton,
            JTextField addressBar,
            JTextArea eventLog) {

        this.eventLog = eventLog; // Speichern des Event-Log-Felds

        // Browser starten
        launchButton.addActionListener(e -> {
            String selectedBrowser = (String) browserSelector.getSelectedItem();
            boolean headless = headlessCheckbox.isSelected();

            if (selectedBrowser != null) {
                try {
                    playwright = Playwright.create();
                    BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(headless);

                    // UI-Parameter setzen
                    setBrowserOptions(selectedBrowser, options, portField, profilePathField, disableGpuCheckbox, noRemoteCheckbox, startMaximizedCheckbox);

                    switch (selectedBrowser.toLowerCase()) {
                        case "chromium":
                            browser = playwright.chromium().launch(options);
                            break;
                        case "firefox":
                            browser = playwright.firefox().launch(options);
                            break;
                        case "webkit":
                            browser = playwright.webkit().launch(options);
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported browser: " + selectedBrowser);
                    }

                    browserContext = browser.newContext();
                    page = browserContext.newPage();

//                    // Event: Console message ✅
//                    page.onConsoleMessage(msg -> {
//                        JOptionPane.showMessageDialog(null, "Console message: " + msg.text());
//                    });
//
                    // Event: Response received ✅
//                    page.onResponse(response -> {
//                        JOptionPane.showMessageDialog(null, "Response received: " + response.url());
//                    });

                    // Event: Page loaded ✅
//                    page.onLoad(p -> {
//                        JOptionPane.showMessageDialog(null, "Page loaded!\n");
//                    });




                    ////////////////////////////////////////////////////////////////////////////////////////////////////

//                    JOptionPane.showMessageDialog(null, selectedBrowser + " erfolgreich gestartet.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Fehler beim Starten des Browsers: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(null, "Bitte einen Browser auswählen.");
            }
        });

        // Browser beenden
        terminateButton.addActionListener(e -> {
            try {
                onCloseBrowser();
//                JOptionPane.showMessageDialog(null, "Browser wurde beendet.");
                System.out.println(" --- Browser wurde beendet ---");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Fehler beim Beenden des Browsers: " + ex.getMessage());
            }
        });

        // URL navigieren
        navigateButton.addActionListener(e -> {
            String url = addressBar.getText();
            if (!url.isEmpty()) {
                try {
                    page.navigate(url);
//                    JOptionPane.showMessageDialog(null, "Navigiere zu: " + url);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Fehler beim Navigieren: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(null, "Bitte eine gültige URL eingeben.");
            }
        });
    }

    private void setBrowserOptions(
            String selectedBrowser,
            BrowserType.LaunchOptions options,
            JTextField portField,
            JTextField profilePathField,
            JCheckBox disableGpuCheckbox,
            JCheckBox noRemoteCheckbox,
            JCheckBox startMaximizedCheckbox
    ) {
        // Kommandozeilenargumente aus der UI
        List<String> browserArgs = new ArrayList<>();
        Map<String, Object> firefoxPrefs = new HashMap<>();

        // Werte aus UI lesen
        String port = portField.getText().trim();
        boolean noRemote = noRemoteCheckbox.isSelected();
        boolean disableGpu = disableGpuCheckbox.isSelected();
        boolean startMaximized = startMaximizedCheckbox.isSelected();
        String profilePath = profilePathField.getText().trim();

        // Port hinzufügen, falls gesetzt
        if (!port.isEmpty()) browserArgs.add("--remote-debugging-port=" + port);

        // UI-Parameter übernehmen
        if (noRemote) browserArgs.add("--no-remote");
        if (disableGpu) browserArgs.add("--disable-gpu");
        if (startMaximized) browserArgs.add("--start-maximized");

        // Profilpfad-Verarbeitung
        if (profilePath == null || profilePath.isEmpty()) {
            // Temporäres Profil verwenden
            String tempProfilePath = System.getProperty("java.io.tmpdir") + "temp_profile_" + System.currentTimeMillis();
            if(selectedBrowser.equalsIgnoreCase("firefox"))
            {
                browserArgs.add("--profile");
                browserArgs.add(tempProfilePath);
            }
            else // Chrome
            {
                browserArgs.add("--user-data-dir=" + tempProfilePath);
            }

            System.out.println("Kein Profil angegeben, temporäres Profil wird verwendet: " + tempProfilePath);
        } else {
        // Benutzerdefiniertes Profil verwenden
            browserArgs.add(selectedBrowser.equalsIgnoreCase("firefox")
                    ? "--profile=" + profilePath
                    : "--user-data-dir=" + profilePath);
        }

        // Falls Firefox gewählt wurde, spezifische User Preferences setzen
        if ("firefox".equalsIgnoreCase(selectedBrowser)) {
            firefoxPrefs.put("browser.startup.homepage", "https://www.google.com"); // Beispiel
            options.setFirefoxUserPrefs(firefoxPrefs);
        }

//        browserArgs.add("--new-instance"); // ToDo: Remove this line, it is only for testing purposes
//        browserArgs.add("--no-remote"); // ToDo: Remove this line, it is only for testing purposes

        // Die gesammelten Argumente setzen
        options.setArgs(browserArgs);
    }


    public byte[] captureScreenshot() {
        return page.screenshot();
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Aktiviert das Event-Logging
     */
    public void startLogging() {
        if (page == null) {
            JOptionPane.showMessageDialog(null, "Browser ist nicht gestartet.");
            return;
        }



        if (!loggingActive) {
            loggingActive = true;
            logEvent("📢 Event-Logging gestartet...");

            // Event: Console message ✅
            page.onConsoleMessage(msg -> logEvent("Console: " + msg.text()));

            // Event: Response received ✅
            page.onResponse(response -> logEvent("Response: " + response.url()));

            // Event: Page loaded ✅
            page.onLoad(p -> logEvent("Page loaded!"));

//            // Event: Klick auf ein Element ✅
//            page.onClick(event -> logEvent("Click on: " + event.target()));
//
//            // Event: Tastatureingabe ✅
//            page.onKeyPress(event -> logEvent("Key Pressed: " + event.key()));
        }
    }

    /**
     * Deaktiviert das Event-Logging
     */
    public void stopLogging() {
        if (page == null) {
            JOptionPane.showMessageDialog(null, "Browser ist nicht gestartet.");
            return;
        }

        if (loggingActive) {
            loggingActive = false;
            logEvent("⏹️ Event-Logging gestoppt.");

            // Event-Listener entfernen
//            page.offConsoleMessage();
//            page.offResponse();
//            page.offLoad();
//            page.offClick();
//            page.offKeyPress();
        }
    }

    /**
     * Fügt eine Nachricht zum Event-Log hinzu.
     */
    private void logEvent(String message) {
        SwingUtilities.invokeLater(() -> eventLog.append(message + "\n"));
    }

}
