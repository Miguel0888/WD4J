package app.controller;

import app.Main;
import wd4j.api.*;
import wd4j.impl.manager.WDScriptManager;
import wd4j.impl.playwright.BrowserImpl;
import wd4j.impl.playwright.PageImpl;
import wd4j.impl.playwright.UserContextImpl;

import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wd4j.impl.webdriver.command.response.WDScriptResult;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.script.*;

public class MainController {

    // SLF4J Logger definieren
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private Playwright playwright;
    private Browser browser;
    private BrowserContext browserContext;
    private UserContextImpl selectedUserContext;
    private Page selectedPage;

    private boolean loggingActive = false; // Status für Play/Pause

    private final Consumer<ConsoleMessage> consoleMessageHandler = msg -> logEvent("Console: " + msg.text());
    private final Consumer<Response> responseHandler = response -> logEvent("Response: " + response.url());
    private final Consumer<Page> loadHandler = p -> logEvent("Page loaded: " + p.url() + " (" + p.title() + ")");

    // Neue Event-Handler
    private final Consumer<Page> closeHandler = p -> logEvent("Page closed!");
    private final Consumer<Page> crashHandler = p -> logEvent("Page crashed!");
    private final Consumer<Dialog> dialogHandler = dialog -> logEvent("Dialog opened: " + dialog.message());
    private final Consumer<Page> domContentLoadedHandler = p -> logEvent("DOM Content Loaded! - " + p.url());
    private final Consumer<Request> requestHandler = req -> logEvent("Request: " + req.url());
    private final Consumer<Request> requestFailedHandler = req -> logEvent("Request Failed: " + req.url());
    private final Consumer<Request> requestFinishedHandler = req -> logEvent("Request Finished: " + req.url());
    private final Consumer<WebSocket> webSocketHandler = ws -> logEvent("WebSocket opened: " + ws.url());
    private final Consumer<Worker> workerHandler = worker -> logEvent("Worker created: " + worker.url());
    private final Consumer<Page> popupHandler = popup -> logEvent("Popup opened!");

    // 🆕 Zusätzliche Event-Handler
    private final Consumer<ConsoleMessage> consoleHandler = msg -> logEvent("Console message: " + msg.text());
    private final Consumer<Page> frameNavigatedHandler = p -> logEvent("Frame navigated: " + p.url());
    private final Consumer<Page> frameAttachedHandler = p -> logEvent("Frame attached!");
    private final Consumer<Page> frameDetachedHandler = p -> logEvent("Frame detached!");
    private final Consumer<Page> downloadHandler = p -> logEvent("File downloaded!");
    private final Consumer<Video> videoHandler = video -> logEvent("Video started recording: " + video.path());

    // Map mit allen Events und den zugehörigen Methoden
    private final Map<String, EventHandler> eventHandlers = new HashMap<>();
    private List<WDScriptResult.AddPreloadScriptResult> addPreloadScriptResults = new ArrayList<>();

    public MainController() {
        logger.info(" *** MainController gestartet! *** ");

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

        // Neue Events hinzufügen
        eventHandlers.put("Close", new EventHandler(
                () -> registerCloseEvent(),
                () -> deregisterCloseEvent()
        ));

        eventHandlers.put("Crash", new EventHandler(
                () -> registerCrashEvent(),
                () -> deregisterCrashEvent()
        ));

        eventHandlers.put("Dialog", new EventHandler(
                () -> registerDialogEvent(),
                () -> deregisterDialogEvent()
        ));

        eventHandlers.put("DOM Content Loaded", new EventHandler(
                () -> registerDOMContentLoadedEvent(),
                () -> deregisterDOMContentLoadedEvent()
        ));

        eventHandlers.put("Request", new EventHandler(
                () -> registerRequestEvent(),
                () -> deregisterRequestEvent()
        ));

        eventHandlers.put("Request Failed", new EventHandler(
                () -> registerRequestFailedEvent(),
                () -> deregisterRequestFailedEvent()
        ));

        eventHandlers.put("Request Finished", new EventHandler(
                () -> registerRequestFinishedEvent(),
                () -> deregisterRequestFinishedEvent()
        ));

        eventHandlers.put("WebSocket", new EventHandler(
                () -> registerWebSocketEvent(),
                () -> deregisterWebSocketEvent()
        ));

        eventHandlers.put("Worker", new EventHandler(
                () -> registerWorkerEvent(),
                () -> deregisterWorkerEvent()
        ));

        eventHandlers.put("Popup", new EventHandler(
                () -> registerPopupEvent(),
                () -> deregisterPopupEvent()
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
            JTextField addressBar) {


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

                    // ToDo:
//                    browserContext = browser.newContext();
//                    page = browserContext.newPage();
                    selectedPage = browser.newPage();
                    updateUserContextDropdown();
                    updateBrowsingContextDropdown();



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
                    selectedPage.navigate(url);
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
        return selectedPage.screenshot();
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Aktiviert das Event-Logging
     */
    public void startLogging() {
        if (selectedPage == null) {
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
        if (selectedPage == null) {
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
        SwingUtilities.invokeLater(() -> Main.console.append(message + "\n"));
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
        SwingUtilities.invokeLater(() -> Main.console.setText(""));
    }

    public void updateBrowsingContextDropdown() {
        SwingUtilities.invokeLater(() -> {
            Main.browsingContextDropdown.removeAllItems();
            Main.browsingContextDropdown.addItem("All"); // Standardwert

            for (String contextId : ((BrowserImpl) browser).getPages().keySet()) {
                Main.browsingContextDropdown.addItem(contextId);
            }
        });
    }

    public void updateUserContextDropdown() {
        SwingUtilities.invokeLater(() -> {
            Main.userContextDropdown.removeAllItems();
            // Der Standardwert ist in der Liste enthalten und muss nicht extra hinzugefügt werden
            for (UserContextImpl userContext : ((BrowserImpl) browser).getUserContextImpls()) {
                Main.userContextDropdown.addItem(userContext.getUserContextId());
            }
        });
    }

    public void updateSelectedPage() {
        String selectedContextId = (String) Main.browsingContextDropdown.getSelectedItem();
        if (selectedContextId == null) {
            return;
        }

        if (selectedContextId.equals("All")) {
            selectedPage = null; // 🔹 Alle Seiten beobachten
        } else {
            selectedPage = ((BrowserImpl) browser).getPages().get(selectedContextId);
        }

        System.out.println("Selected Page updated: " + (selectedPage != null ? ((PageImpl) selectedPage) : "All"));
    }

    public void updateSelectedUserContext() {
        String selectedContextId = (String) Main.userContextDropdown.getSelectedItem();
        if(selectedContextId == null)
        {
            return;
        }
        if (selectedContextId.equals("default")) {
            selectedUserContext = null;
        } else {
            selectedUserContext = ((BrowserImpl) browser).getUserContextImpls().stream()
                    .filter(ctx -> ctx.getUserContextId().equals(selectedContextId))
                    .findFirst()
                    .orElse(null);
        }
        System.out.println("Selected User Context updated: " + (selectedUserContext != null ? ((UserContextImpl) selectedUserContext).getUserContextId() : "default"));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ToDo: Improve this part:

    /** Registrierungs-Methoden */
    private void registerConsoleLogEvent() {
        if (selectedPage != null) selectedPage.onConsoleMessage(consoleMessageHandler);
    }

    private void deregisterConsoleLogEvent() {
        if (selectedPage != null) selectedPage.offConsoleMessage(consoleMessageHandler);
    }

    private void registerNetworkResponseEvent() {
        if (selectedPage != null) selectedPage.onResponse(responseHandler);
    }

    private void deregisterNetworkResponseEvent() {
        if (selectedPage != null) selectedPage.offResponse(responseHandler);
    }

    private void registerPageLoadEvent() {
        if (selectedPage != null) selectedPage.onLoad(loadHandler);
    }

    private void deregisterPageLoadEvent() {
        if (selectedPage != null) selectedPage.offLoad(loadHandler);
    }

    /** Neue Registrierungs-Methoden */
    private void registerCloseEvent() {
        if (selectedPage != null) selectedPage.onClose(closeHandler);
    }

    private void deregisterCloseEvent() {
        if (selectedPage != null) selectedPage.offClose(closeHandler);
    }

    private void registerCrashEvent() {
        if (selectedPage != null) selectedPage.onCrash(crashHandler);
    }

    private void deregisterCrashEvent() {
        if (selectedPage != null) selectedPage.offCrash(crashHandler);
    }

    private void registerDialogEvent() {
        if (selectedPage != null) selectedPage.onDialog(dialogHandler);
    }

    private void deregisterDialogEvent() {
        if (selectedPage != null) selectedPage.offDialog(dialogHandler);
    }

    private void registerDOMContentLoadedEvent() {
        if (selectedPage != null) selectedPage.onDOMContentLoaded(domContentLoadedHandler);
    }

    private void deregisterDOMContentLoadedEvent() {
        if (selectedPage != null) selectedPage.offDOMContentLoaded(domContentLoadedHandler);
    }

    private void registerRequestEvent() {
        if (selectedPage != null) selectedPage.onRequest(requestHandler);
    }

    private void deregisterRequestEvent() {
        if (selectedPage != null) selectedPage.offRequest(requestHandler);
    }

    private void registerRequestFailedEvent() {
        if (selectedPage != null) selectedPage.onRequestFailed(requestFailedHandler);
    }

    private void deregisterRequestFailedEvent() {
        if (selectedPage != null) selectedPage.offRequestFailed(requestFailedHandler);
    }

    private void registerRequestFinishedEvent() {
        if (selectedPage != null) selectedPage.onRequestFinished(requestFinishedHandler);
    }

    private void deregisterRequestFinishedEvent() {
        if (selectedPage != null) selectedPage.offRequestFinished(requestFinishedHandler);
    }

    private void registerWebSocketEvent() {
        if (selectedPage != null) selectedPage.onWebSocket(webSocketHandler);
    }

    private void deregisterWebSocketEvent() {
        if (selectedPage != null) selectedPage.offWebSocket(webSocketHandler);
    }

    private void registerWorkerEvent() {
        if (selectedPage != null) selectedPage.onWorker(workerHandler);
    }

    private void deregisterWorkerEvent() {
        if (selectedPage != null) selectedPage.offWorker(workerHandler);
    }

    private void registerPopupEvent() {
        if (selectedPage != null) selectedPage.onPopup(popupHandler);
    }

    private void deregisterPopupEvent() {
        if (selectedPage != null) selectedPage.offPopup(popupHandler);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Deprecated // since it does not activate the tooltip for all pages
    public void showSelectors(boolean selected, Page selectedPage) {
        if (selectedPage != null) {
            List<WDLocalValue> args = new ArrayList<>();
            args.add(new WDPrimitiveProtocolValue.BooleanValue(selected)); // true oder false übergeben

            WDScriptManager.getInstance().callFunction(
                    "toggleTooltip",
                    false, // awaitPromise=false
                    new WDTarget.ContextTarget(new WDBrowsingContext(((PageImpl) selectedPage).getBrowsingContextId())), // Ziel: aktuelle Seite
                    args
            );
        }
    }

    public void runScript(String script) {
        String text = Main.scriptLog.getText();

        // ToDo: Implement this method

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void showSelectors(boolean selected) {
        // 1. Alle Realms abrufen
        WDScriptManager scriptManager = WDScriptManager.getInstance();
        WDScriptResult.GetRealmsResult realmsResult = scriptManager.getRealms(); // Hol alle existierenden Realms

        // 2. Für jeden Context toggleTooltip aktivieren
        for (WDRealmInfo realm : realmsResult.getRealms()) {
            List<WDLocalValue> args = new ArrayList<>();
            args.add(new WDPrimitiveProtocolValue.BooleanValue(selected)); // aktivieren oder deaktivieren

            scriptManager.callFunction(
                    "toggleTooltip",
                    false, // awaitPromise=false
                    new WDTarget.RealmTarget(new WDRealm(realm.getRealm())), // Kontext: Aktueller Realm
                    args
            );
        }
    }

    public void showDomEvents(boolean selected) {
        // 1. Alle Realms abrufen
        WDScriptManager scriptManager = WDScriptManager.getInstance();
        WDScriptResult.GetRealmsResult realmsResult = scriptManager.getRealms(); // Hol alle existierenden Realms

        // 2. Für jeden Context toggleTooltip aktivieren
        for (WDRealmInfo realm : realmsResult.getRealms()) {
            List<WDLocalValue> args = new ArrayList<>();
            args.add(new WDPrimitiveProtocolValue.BooleanValue(selected)); // aktivieren oder deaktivieren

            scriptManager.callFunction(
                    "toggleDomObserver",
                    false, // awaitPromise=false
                    new WDTarget.RealmTarget(new WDRealm(realm.getRealm())), // Kontext: Aktueller Realm
                    args
            );
        }
    }
}
