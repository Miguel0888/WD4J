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
import wd4j.impl.support.Pages;
import wd4j.impl.webdriver.command.response.WDScriptResult;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.script.*;

public class MainController {

    // SLF4J Logger definieren
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private Playwright playwright;
    private BrowserImpl browser;
    private BrowserContext browserContext;
    private UserContextImpl selectedUserContext;

    private boolean isEventLoggingEnabled = true; // Status für Play/Pause

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
                            browser = (BrowserImpl) playwright.chromium().launch(options);
                            break;
                        case "firefox":
                            browser = (BrowserImpl) playwright.firefox().launch(options);
                            break;
                        case "webkit":
                            browser = (BrowserImpl) playwright.webkit().launch(options);
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported browser: " + selectedBrowser);
                    }

                    setupPageListeners();  // sorgt dafür, dass neue Seiten automatisch hinzugefügt oder entfernt werden
                    updateUserContextDropdown();
                    updateBrowsingContextDropdown();

                    // Setzt das BrowsingContext-Dropdown automatisch auf den aktiven Context
//                    browser.onContextSwitch(page -> {
//                        SwingUtilities.invokeLater(() -> {
//                            updateSelectedBrowsingContext(page.getBrowsingContextId());
//                        });
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
                    browser.getPages().getActivePage().navigate(url);
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
        return browser.getPages().getActivePage().screenshot();
    }

    ////////////////////////////////////////////////////////////////////////////

    public void setEventLoggingEnabled(boolean enabled) {
        isEventLoggingEnabled = enabled;
    }

    /**
     * Fügt eine Nachricht zum Event-Log hinzu.
     */
    private void logEvent(String message) {
       if(isEventLoggingEnabled)
       {
           SwingUtilities.invokeLater(() -> Main.getConsoleTab().appendLog(message + "\n"));
       }
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
        Main.getConsoleTab().clearLog();
    }

    public void updateBrowsingContextDropdown() {
        SwingUtilities.invokeLater(() -> {
            JComboBox<Object> dropdown = Main.getSettingsTab().getBrowsingContextDropdown();
            DefaultComboBoxModel<Object> model = (DefaultComboBoxModel<Object>) dropdown.getModel();

            Set<String> newItems = browser.getPages().keySet();
            Set<String> currentItems = new HashSet<>();

            // 🔹 1️⃣ Bestehende Elemente sammeln
            for (int i = 0; i < model.getSize(); i++) {
                System.out.println("Element: " + model.getElementAt(i));
                currentItems.add(model.getElementAt(i).toString());
            }

            // 🔹 2️⃣ Fehlende Elemente hinzufügen
            for (String contextId : newItems) {
                if (!currentItems.contains(contextId)) {
                    System.out.println("Adding new context: " + contextId);
                    model.addElement(contextId);
                }
            }

            // 🔹 3️⃣ Überflüssige Elemente entfernen
            for (String existingItem : new HashSet<>(currentItems)) {
                if (!newItems.contains(existingItem)) {
                    System.out.println("Removing context: " + existingItem);
                    model.removeElement(existingItem);
                }
            }
        });
    }


    public void updateUserContextDropdown() {
        SwingUtilities.invokeLater(() -> {
            JComboBox<Object> userDropdown = Main.getSettingsTab().getUserContextDropdown();
            userDropdown.removeAllItems();
            for (UserContextImpl userContext : browser.getUserContextImpls()) {
                userDropdown.addItem(userContext.getUserContextId());
            }
        });
    }

    public void switchSelectedPage() {
        String selectedContextId = (String) Main.getSettingsTab().getBrowsingContextDropdown().getSelectedItem();
        if(!Objects.equals(selectedContextId, browser.getPages().getActivePageId()))
        { // avoid infinite event loop
            browser.getPages().setActivePageId(selectedContextId, true);
        }
    }

    public void updateSelectedUserContext() {
        String selectedContextId = (String) Main.getSettingsTab().getUserContextDropdown().getSelectedItem();
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

    ///
    // ToDo: Improve this part:
    private void registerConsoleLogEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().onConsoleMessage(consoleMessageHandler);
    }

    private void deregisterConsoleLogEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().offConsoleMessage(consoleMessageHandler);
    }

    private void registerNetworkResponseEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().onResponse(responseHandler);
    }

    private void deregisterNetworkResponseEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().offResponse(responseHandler);
    }

    private void registerPageLoadEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().onLoad(loadHandler);
    }

    private void deregisterPageLoadEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().offLoad(loadHandler);
    }

    /** Neue Registrierungs-Methoden */
    private void registerCloseEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().onClose(closeHandler);
    }

    private void deregisterCloseEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().offClose(closeHandler);
    }

    private void registerCrashEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().onCrash(crashHandler);
    }

    private void deregisterCrashEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().offCrash(crashHandler);
    }

    private void registerDialogEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().onDialog(dialogHandler);
    }

    private void deregisterDialogEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().offDialog(dialogHandler);
    }

    private void registerDOMContentLoadedEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().onDOMContentLoaded(domContentLoadedHandler);
    }

    private void deregisterDOMContentLoadedEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().offDOMContentLoaded(domContentLoadedHandler);
    }

    private void registerRequestEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().onRequest(requestHandler);
    }

    private void deregisterRequestEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().offRequest(requestHandler);
    }

    private void registerRequestFailedEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().onRequestFailed(requestFailedHandler);
    }

    private void deregisterRequestFailedEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().offRequestFailed(requestFailedHandler);
    }

    private void registerRequestFinishedEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().onRequestFinished(requestFinishedHandler);
    }

    private void deregisterRequestFinishedEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().offRequestFinished(requestFinishedHandler);
    }

    private void registerWebSocketEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().onWebSocket(webSocketHandler);
    }

    private void deregisterWebSocketEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().offWebSocket(webSocketHandler);
    }

    private void registerWorkerEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().onWorker(workerHandler);
    }

    private void deregisterWorkerEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().offWorker(workerHandler);
    }

    private void registerPopupEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().onPopup(popupHandler);
    }

    private void deregisterPopupEvent() {
        if (browser.getPages().getActivePage()!= null) browser.getPages().getActivePage().offPopup(popupHandler);
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
        String text = Main.getScriptTab().getScriptLog();

        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Script is empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // ToDo: Implement script execution (gedacht waren webdriver bidi scripts nicht nur JS Scripts)
            // Beispiel: Ein einfaches JavaScript-Snippet ausführen
            browser.getPages().getActivePage().evaluate(script);

            // Log-Eintrag aktualisieren
            SwingUtilities.invokeLater(() -> Main.getScriptTab().appendLog("Executed script: \n" + script + "\n"));
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> Main.getScriptTab().appendLog("Error executing script: " + ex.getMessage() + "\n"));
        }
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

    public void createBrowsingContext() {
        browser.newPage();
        // ToDo: activate new page
    }

    public void goBack() {
        browser.getPages().getActivePage().goBack();
    }

    public void goForward() {
        browser.getPages().getActivePage().goForward();
    }

    public void reload() {
        browser.getPages().getActivePage().reload();
    }

    public void closePage() {
        // Ask for confirmation
        int result = JOptionPane.showConfirmDialog(null,
                "Möchten Sie den aktuellen Browsing Context wirklich schließen?",
                "Browsing Context schließen",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            PageImpl activePage = browser.getPages().getActivePage();
            if(activePage != null) {
                activePage.close();
            }
        }
    }

    private void setupPageListeners() {
        Pages pages = ((BrowserImpl) browser).getPages();

        // 🔥 1. Event: BrowsingContext-Update (Liste neu laden)
        pages.addListener(evt -> {
            if (Pages.EventType.BROWSING_CONTEXT_ADDED.name().equals(evt.getPropertyName())) {
                SwingUtilities.invokeLater(() -> {
                    String newContextId = (String) evt.getNewValue();
                    Main.getSettingsTab().getBrowsingContextDropdown().addItem(newContextId);
                });
            }
        });

        // 🔥 2. Event: BrowsingContext-Update (Liste neu laden)
        pages.addListener(evt -> {
            if (Pages.EventType.BROWSING_CONTEXT_REMOVED.name().equals(evt.getPropertyName())) {
                SwingUtilities.invokeLater(() -> {
                    String removedContextId = (String) evt.getNewValue();
                    Main.getSettingsTab().getBrowsingContextDropdown().removeItem(removedContextId);
//                    updateBrowsingContextDropdown();
                });
            }
        });

        // 🔥 3. Event: Aktive Seite setzen (Dropdown aktualisieren)
        pages.addListener(evt -> {
            if (Pages.EventType.ACTIVE_PAGE_CHANGED.name().equals(evt.getPropertyName())) {
                SwingUtilities.invokeLater(() -> {
                    String newContextId = (String) evt.getNewValue();

                    JComboBox<Object> dropdown = Main.getSettingsTab().getBrowsingContextDropdown();
                    DefaultComboBoxModel<Object> model = (DefaultComboBoxModel<Object>) dropdown.getModel();

                    if (newContextId != null) {

                        boolean exists = false;

                        // 🔹 1️⃣ Prüfen, ob `newContextId` bereits im Dropdown existiert
                        for (int i = 0; i < model.getSize(); i++) {
                            if (newContextId.equals(model.getElementAt(i))) {
                                exists = true;
                                break;
                            }
                        }

                        // 🔹 2️⃣ Falls nicht vorhanden, zuerst hinzufügen
                        if (!exists) {
                            model.addElement(newContextId);
                        }

                        System.out.println("-------> Aktive Seite geändert: " + newContextId);

                        // 🔹 3️⃣ Dann auswählen
                        dropdown.setSelectedItem(newContextId);
                    }
                });
            }
        });
    }

}
