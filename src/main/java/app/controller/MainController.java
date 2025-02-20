package app.controller;

import wd4j.api.*;

import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;

public class MainController {
    private Playwright playwright;
    private Browser browser;
    private BrowserContext browserContext;
    private Page page;

    private JTextArea eventLog;
    private boolean loggingActive = false; // Status für Play/Pause

    private final Consumer<ConsoleMessage> consoleMessageHandler = msg -> logEvent("Console: " + msg.text());
    private final Consumer<Response> responseHandler = response -> logEvent("Response: " + response.url());
    private final Consumer<Page> loadHandler = p -> logEvent("Page loaded!");

    // Map mit allen Events und den zugehörigen Methoden
    private final Map<String, EventHandler> eventHandlers = new HashMap<>();

    public MainController() {
        eventHandlers.put("Console Log", new EventHandler(
                () -> registerConsoleLogEvent(),
                () -> deregisterConsoleLogEvent()
        ));

        eventHandlers.put("Network Response", new EventHandler(
                () -> registerNetworkResponseEvent(),
                () -> deregisterNetworkResponseEvent()
        ));

        eventHandlers.put("Page Loaded", new EventHandler(
                () -> registerPageLoadEvent(),
                () -> deregisterPageLoadEvent()
        ));
    }

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
            JCheckBox useProfileCheckbox,
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
                    setBrowserOptions(selectedBrowser, options, portField, useProfileCheckbox, profilePathField, disableGpuCheckbox, noRemoteCheckbox, startMaximizedCheckbox);

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
            JCheckBox useProfileCheckbox, JTextField profilePathField,
            JCheckBox disableGpuCheckbox,
            JCheckBox noRemoteCheckbox,
            JCheckBox startMaximizedCheckbox
    ) {
        // Kommandozeilenargumente aus der UI
        List<String> browserArgs = new ArrayList<>();
        Map<String, Object> firefoxPrefs = new HashMap<>();

        // Port hinzufügen, falls gesetzt
        String port = portField.getText().trim();
        if (!port.isEmpty()) browserArgs.add("--remote-debugging-port=" + port);

        // UI-Parameter übernehmen
        if (noRemoteCheckbox.isSelected()) browserArgs.add("--no-remote");
        if (disableGpuCheckbox.isSelected()) browserArgs.add("--disable-gpu");
        if (startMaximizedCheckbox.isSelected()) browserArgs.add("--start-maximized");

        // Profilpfad-Verarbeitung
        if(useProfileCheckbox.isSelected()) {
            if (profilePathField.getText().trim().isEmpty()) {
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
                if(selectedBrowser.equalsIgnoreCase("firefox"))
                {
                    browserArgs.add("--profile");
                    browserArgs.add(profilePathField.getText().trim());
                }
                else // Chrome
                {
                    browserArgs.add("--user-data-dir=" + profilePathField.getText());
                }
            }
        }

        // ToDo: Implement User Preferences for Firefox
        // Falls Firefox gewählt wurde, spezifische User Preferences setzen
        if ("firefox".equalsIgnoreCase(selectedBrowser)) {
            firefoxPrefs.put("browser.startup.homepage", "https://www.google.com"); // Beispiel
            options.setFirefoxUserPrefs(firefoxPrefs);
        }

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

            //ToDo
//            // Event: Console message ✅
//            page.onConsoleMessage(consoleMessageHandler);
//
//            // Event: Response received ✅
//            page.onResponse(responseHandler);
//
//            // Event: Page loaded ✅
//            page.onLoad(loadHandler);
//
////            // Event: Klick auf ein Element ✅
////            page.onClick(event -> logEvent("Click on: " + event.target()));
////
////            // Event: Tastatureingabe ✅
////            page.onKeyPress(event -> logEvent("Key Pressed: " + event.key()));
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

            // ToDo
//            // Event-Listener entfernen
//            page.offConsoleMessage(consoleMessageHandler);
//            page.offResponse(responseHandler);
//            page.offLoad(loadHandler);
//            //            page.offClick();
//            //            page.offKeyPress();
        }
    }

    /**
     * Fügt eine Nachricht zum Event-Log hinzu.
     */
    private void logEvent(String message) {
        SwingUtilities.invokeLater(() -> eventLog.append(message + "\n"));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Map<String, EventHandler> getEventHandlers() {
        return eventHandlers;
    }

    public void registerEvent(String eventName) {
        EventHandler handler = eventHandlers.get(eventName);
        if (handler != null) {
            handler.register();
            System.out.println("Event aktiviert: " + eventName);
        }
    }

    public void deregisterEvent(String eventName) {
        EventHandler handler = eventHandlers.get(eventName);
        if (handler != null) {
            handler.deregister();
            System.out.println("Event deaktiviert: " + eventName);
        }
    }

    public void clearLog() {
        SwingUtilities.invokeLater(() -> eventLog.setText(""));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ToDo: Improve this part:

    /** Registrierungs-Methoden */
    private void registerConsoleLogEvent() {
        if (page != null) page.onConsoleMessage(consoleMessageHandler);
    }

    private void deregisterConsoleLogEvent() {
        if (page != null) page.offConsoleMessage(consoleMessageHandler);
    }

    private void registerNetworkResponseEvent() {
        if (page != null) page.onResponse(responseHandler);
    }

    private void deregisterNetworkResponseEvent() {
        if (page != null) page.offResponse(responseHandler);
    }

    private void registerPageLoadEvent() {
        if (page != null) page.onLoad(loadHandler);
    }

    private void deregisterPageLoadEvent() {
        if (page != null) page.offLoad(loadHandler);
    }

}
