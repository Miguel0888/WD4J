package de.bund.zrb.service;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.BrowserLifecycleEvent;
import com.microsoft.playwright.*;
import de.bund.zrb.*;
import de.bund.zrb.auth.LoginRedirectSentry;
import de.bund.zrb.command.response.WDScriptResult;
import de.bund.zrb.manager.WDScriptManager;
import de.bund.zrb.type.script.WDLocalValue;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;
import de.bund.zrb.type.script.WDRealmInfo;
import de.bund.zrb.type.script.WDTarget;
import de.bund.zrb.util.GrowlNotificationPopupUtil;
import de.bund.zrb.win.BrowserInstanceState;
import de.bund.zrb.win.BrowserProcessService;
import de.bund.zrb.win.BrowserTerminationResult;
import de.bund.zrb.win.WindowsBrowserProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.*;

public class BrowserServiceImpl implements BrowserService {

    private final Map<String, BrowserContext> userContexts = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(BrowserServiceImpl.class);
    private static final BrowserServiceImpl INSTANCE = new BrowserServiceImpl();

    private Playwright playwright;
    private BrowserImpl browser;

    private final List<ActivePageListener> activePageListeners = new ArrayList<>();
    private LoginRedirectSentry autoLogin;
    private final BrowserProcessService browserProcessService = new WindowsBrowserProcessService();

    private BrowserServiceImpl() {}

    public static BrowserServiceImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public void launchBrowser(BrowserConfig config) {
        ApplicationEventBus.getInstance().publish(new BrowserLifecycleEvent(new BrowserLifecycleEvent.Payload(BrowserLifecycleEvent.Kind.STARTING, "🚀 Browser wird gestartet…")));
        try {
            // 1) Vor jeglichem Start prüfen & ggf. terminieren
            if (!detectAndOptionallyTerminateRunningBrowser(config)) {
                // Benutzer hat abgebrochen
                return;
            }

            // 2) Playwright initialisieren erst nach Freigabe
            playwright = Playwright.create();
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(config.isHeadless());

            List<String> args = new ArrayList<>();
            if (config.getPort() > 0) {
                args.add("--remote-debugging-port=" + config.getPort());
            }
            if (config.isNoRemote()) {
                args.add("--no-remote");
            }
            if (config.isDisableGpu()) {
                args.add("--disable-gpu");
            }
            if (config.isStartMaximized()) {
                args.add("--start-maximized");
            }
            if (config.isUseProfile()) {
                if (config.getProfilePath() != null && !config.getProfilePath().isEmpty()) {
                    args.add("--user-data-dir=" + config.getProfilePath());
                } else {
                    args.add("--user-data-dir=" + System.getProperty("java.io.tmpdir") + "temp_profile_" + System.currentTimeMillis());
                }
            }
            if (config.getExtraArgs() != null && !config.getExtraArgs().trim().isEmpty()) {
                for (String a : config.getExtraArgs().trim().split("\\s+")) {
                    if (!a.isEmpty()) args.add(a);
                }
            }

            if (args.isEmpty()) {
                throw new IllegalArgumentException("Keine Startargumente gesetzt. Stelle sicher, dass die UI-Optionen korrekt übergeben werden.");
            }

            options.setArgs(args);

            if ("firefox".equalsIgnoreCase(config.getBrowserType())) {
                Map<String, Object> firefoxPrefs = new HashMap<>();
                firefoxPrefs.put("browser.startup.homepage", "https://www.google.com");
                options.setFirefoxUserPrefs(firefoxPrefs);
            }

            Double websocketTimeout = SettingsService.getInstance().get("websocketTimeout", Double.class);
            options.setTimeout(websocketTimeout != null ? websocketTimeout : 0);
            BrowserTypeImpl browserType;
            switch (config.getBrowserType().toLowerCase()) {
                case "chromium": browserType = BrowserTypeImpl.newChromiumInstance((PlaywrightImpl) playwright); break;
                case "edge": browserType = BrowserTypeImpl.newEdgeInstance((PlaywrightImpl) playwright); break;
                case "firefox":
                default: browserType = BrowserTypeImpl.newFirefoxInstance((PlaywrightImpl) playwright); break;
            }
            browser = (BrowserImpl) browserType.launch(options);
            configureServices();
            // ↙︎ Externes Schließen erkennen
            browser.onDisconnected(new java.util.function.Consumer<Browser>() {
                @Override
                public void accept(Browser b) {
                    handleExternalBrowserClosed();
                }
            });
            ApplicationEventBus.getInstance().publish(new BrowserLifecycleEvent(new BrowserLifecycleEvent.Payload(BrowserLifecycleEvent.Kind.STARTED, "✅ Browser gestartet")));

            // Optional: pro Benutzer automatisch Startseite öffnen (neuer Tab)
            try {
                for (UserRegistry.User u : UserRegistry.getInstance().getAll()) {
                    if (u.isAutoOpenStartPageOnLaunch()) {
                        String url = u.getStartPage();
                        if (url != null && !url.trim().isEmpty()) {
                          Page page = createNewTab(u.getUsername());
                          if (page != null) page.navigate(url.trim());
                        }
                    }
                }
            } catch (Throwable t) {
                logger.warn("Auto-Navigation zur Startseite fehlgeschlagen", t);
            }
        } catch (Exception ex) {
            // StatusBar-Fehler inkl. Retry-Button
            ApplicationEventBus.getInstance().publish(
                    new BrowserLifecycleEvent(new BrowserLifecycleEvent.Payload(
                            BrowserLifecycleEvent.Kind.ERROR,
                            "❌ Browser-Start fehlgeschlagen: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()),
                            ex,
                            new BrowserLifecycleEvent.Action("Erneut versuchen", () -> {
                                try { launchBrowser(config); } catch (Throwable t) { /* erneuter Fehler wird separat via Event gemeldet */ }
                            })
                    ))
            );
            return; // Entfernt Hinweis: notwendig hier für frühzeitiges Abbrechen
        }
    }

    private void handleExternalBrowserClosed() {
        // Event mit Restart-Button publizieren
        ApplicationEventBus.getInstance().publish(
                new BrowserLifecycleEvent(new BrowserLifecycleEvent.Payload(
                        BrowserLifecycleEvent.Kind.EXTERNALLY_CLOSED,
                        "🔌 Browser-Fenster wurde extern geschlossen",
                        new BrowserLifecycleEvent.Action("Neu starten", () -> {
                            try { launchDefaultBrowser(); } catch (Throwable t) { /* Fehler werden separat über Events gemeldet */ }
                        })
                ))
        );
        // Aufräumen, aber idempotent
        try { if (browser != null) { try { browser.close(); } catch (Throwable ignore) {} } } catch (Throwable ignore) {}
        try { if (playwright != null) { try { playwright.close(); } catch (Throwable ignore) {} } } catch (Throwable ignore) {}
        browser = null;
        playwright = null;
    }

    /**
     * Startet den Browser mit einer vernünftigen Default-Konfiguration (Firefox, non-headless, Port 9222 etc.).
     * Dient als Ersatz für die bisherige initBrowser()-Logik aus der UI.
     */
    public void launchDefaultBrowser() {
        SettingsService s = SettingsService.getInstance();
        BrowserConfig config = new BrowserConfig();
        String sel = s.get("browser.selected", String.class);
        config.setBrowserType(sel != null ? sel : "firefox");
        Integer p = s.get("browser.port", Integer.class); config.setPort(p != null ? p : 9222);
        Boolean headless = s.get("browser.headless", Boolean.class); config.setHeadless(headless != null ? headless : false);
        Boolean disGpu = s.get("browser.disableGpu", Boolean.class); config.setDisableGpu(disGpu != null ? disGpu : false);
        Boolean noRem = s.get("browser.noRemote", Boolean.class); config.setNoRemote(noRem != null ? noRem : false);
        Boolean startMax = s.get("browser.startMaximized", Boolean.class); config.setStartMaximized(startMax != null ? startMax : true);
        Boolean useProf = s.get("browser.useProfile", Boolean.class); config.setUseProfile(useProf != null ? useProf : false);
        String profPath = s.get("browser.profilePath", String.class); config.setProfilePath(profPath);
        String extra = s.get("browser.extraArgs", String.class); config.setExtraArgs(extra);
        launchBrowser(config);
    }

    private void configureServices() {
        NotificationService.getInstance(browser); // init
        GrowlNotificationPopupUtil.hook(browser); // <<< einmalig für notifications registrieren

        ActivityService.getInstance(browser); // init
        VideoRecordingService.getInstance().init((BrowserImpl) browser);

        // Auto-Login global aktivieren
        autoLogin = new LoginRedirectSentry(browser, ToolsRegistry.getInstance().loginTool());
        autoLogin.enable();

        // 🔌 Fokus → aktuellen Benutzer umschalten
        browser.onContextSwitch(ctxId -> {
            // 1) Zentral setzen: feuert PropertyChange "currentUser"
            de.bund.zrb.service.UserContextMappingService.getInstance().setCurrentUserByContextId(ctxId);

            // 2) (optional) Statuszeile updaten
            UserRegistry.User u = userForBrowsingContextId(ctxId); // User zum Kontext bestimmen
            SwingUtilities.invokeLater(() -> {
                String name = (u == null) ? "<Keinen>" : u.getUsername();
                // optional: StatusBar aktualisieren
            });
        });
    }

    @Override
    public void terminateBrowser() {
        ApplicationEventBus.getInstance().publish(new BrowserLifecycleEvent(new BrowserLifecycleEvent.Payload(BrowserLifecycleEvent.Kind.STOPPING, "🛑 Browser wird beendet…")));
        // NPE-sicheres Beenden – getrennte Null-Checks und try-catch
        try {
            if (browser != null) {
                try { browser.close(); } catch (Throwable ignore) {}
            }
        } finally {
            browser = null;
        }
        try {
            if (playwright != null) {
                try { playwright.close(); } catch (Throwable ignore) {}
            }
        } finally {
            playwright = null;
        }
        ApplicationEventBus.getInstance().publish(new BrowserLifecycleEvent(new BrowserLifecycleEvent.Payload(BrowserLifecycleEvent.Kind.STOPPED, "🛑 Browser beendet")));
    }

    @Override
    public void navigate(String url) {
        browser.getActivePage().navigate(url);
    }

    @Override
    public void createNewTab() {
        browser.newPage();
    }

    @Override
    public String createUserContext(UserRegistry.User user) {
        getOrCreateUserContext(user.getUsername());
        return user.getUsername();
    }

    @Override
    public void closeUserContext(String username) {
        BrowserContext context = userContexts.remove(username);
        if (context != null) {
            context.close();
        }
        UserContextMappingService.getInstance().remove(username);
    }

    private BrowserContext getOrCreateUserContext(String username) {
        return userContexts.computeIfAbsent(username, u -> {
            if (browser == null) {
                throw new IllegalStateException("Browser ist nicht gestartet!");
            }

            BrowserContext context = browser.newContext();

            // Lookup User für Mapping
            UserRegistry.User user = UserRegistry.getInstance().getAll().stream()
                    .filter(it -> it.getUsername().equals(u))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unbekannter Benutzer: " + u));

            UserContextMappingService.getInstance().bindUserToContext(username, context, user);

            return context;
        });
    }

    @Override
    public com.microsoft.playwright.Page getActivePage(String username) {
        BrowserContext context = getOrCreateUserContext(username);
        List<Page> pages = context.pages();
        if (pages.isEmpty()) {
            return context.newPage();
        }
        return pages.get(0);
    }

    @Override
    public com.microsoft.playwright.Page getActivePage() {
        return browser.getActivePage();
    }

    @Override
    public com.microsoft.playwright.Page createNewTab(String username) {
        BrowserContext context = getOrCreateUserContext(username);
        return context.newPage();
    }

    @Override
    public void closeActiveTab() {
        PageImpl activePage = browser.getActivePage();
        if (activePage != null) {
            activePage.close();
        }
    }

    @Override
    public void closeActiveTab(String username) {
        Page page = getActivePage(username);
        page.close();
    }

    @Override
    public void goBack() {
        browser.getActivePage().goBack();
    }

    @Override
    public void goBack(String username) {
        getActivePage(username).goBack();
    }

    @Override
    public void goForward() {
        browser.getActivePage().goForward();
    }

    @Override
    public void goForward(String username) {
        getActivePage(username).goForward();
    }

    @Override
    public void reload() {
        browser.getActivePage().reload();
    }

    @Override
    public void reload(String username) {
        getActivePage(username).reload();
    }

    @Override
    public byte[] captureScreenshot() {
        return browser.getActivePage().screenshot();
    }

    @Override
    public void showSelectors(boolean selected) {
        WDScriptManager scriptManager = browser.getWebDriver().script();
        WDScriptResult.GetRealmsResult realmsResult = scriptManager.getRealms();

        for (WDRealmInfo realm : realmsResult.getRealms()) {
            List<WDLocalValue> args = new ArrayList<>();
            args.add(new WDPrimitiveProtocolValue.BooleanValue(selected));

            scriptManager.callFunction("toggleTooltip", false,
                    new WDTarget.RealmTarget(realm.getRealm()), args);
        }
    }

    @Override
    public void showDomEvents(boolean selected) {
        WDScriptManager scriptManager = browser.getWebDriver().script();
        WDScriptResult.GetRealmsResult realmsResult = scriptManager.getRealms();

        for (WDRealmInfo realm : realmsResult.getRealms()) {
            List<WDLocalValue> args = new ArrayList<>();
            args.add(new WDPrimitiveProtocolValue.BooleanValue(selected));

            scriptManager.callFunction("toggleDomObserver", false,
                    new WDTarget.RealmTarget(realm.getRealm()), args);
        }
    }

    public void switchSelectedPage(String newContextId) {
        if (!Objects.equals(newContextId, browser.getActivePageId())) {
            browser.getWebDriver().browsingContext().activate(newContextId);
            browser.setActivePageId(newContextId);
            notifyActivePageChanged(newContextId);
        }
    }

    public void addActivePageListener(ActivePageListener listener) {
        activePageListeners.add(listener);
    }

    public void removeActivePageListener(ActivePageListener listener) {
        activePageListeners.remove(listener);
    }

    private void notifyActivePageChanged(String newContextId) {
        for (ActivePageListener listener : activePageListeners) {
            listener.onActivePageChanged(newContextId);
        }
    }

    @Override
    public BrowserImpl getBrowser() {
        return browser;
    }

    public RecordingEventRouter getRecordingEventRouter() {
        if (browser == null) {
            return null;
        }
        return browser.getRecordingEventRouter();
    }

    @Override
    public Page pageForBrowsingContextId(String browsingContextId) {
        if (browser == null) return null;
        if (browsingContextId == null || browsingContextId.isEmpty()) {
            return browser.getActivePage(); // Fallback: aktive Seite
        }

        // Fast-Path: aktive Seite per ID
        if (Objects.equals(browsingContextId, browser.getActivePageId())) {
            return browser.getActivePage();
        }

        // Besitzer-UserContext finden und dort die Page holen
        for (de.bund.zrb.UserContextImpl uc : browser.getUserContextImpls()) {
            if (uc.hasPage(browsingContextId)) {
                // ↓ diese Methode in UserContextImpl ergänzen (siehe unten)
                Page p = uc.getPage(browsingContextId);
                if (p != null) return p;
            }
        }

        // Nichts gefunden
        return null;
    }

    public interface ActivePageListener {
        void onActivePageChanged(String contextId);
    }

    ///////////////////////////

    // BrowserImpl.java – unter die anderen Methoden hinzufügen
    public de.bund.zrb.service.UserRegistry.User userForBrowsingContextId(String browsingContextId) {
        if (browsingContextId == null || browsingContextId.isEmpty()) {
            return de.bund.zrb.service.UserContextMappingService.getInstance().getCurrentUser();
        }

        // 1) passenden UserContextImpl (Tab-Container) zu dieser BrowsingContext-ID finden
        de.bund.zrb.UserContextImpl owningUc = null;
        for (de.bund.zrb.UserContextImpl uc : browser.getUserContextImpls()) {
            if (uc.hasPage(browsingContextId)) {
                owningUc = uc;
                break;
            }
        }
        if (owningUc == null) {
            // Fallback: aktiver User (UI-Auswahl) – besser als null
            return de.bund.zrb.service.UserContextMappingService.getInstance().getCurrentUser();
        }

        // 2) User zu diesem BrowserContext herleiten, ohne den Service zu erweitern:
        //    Wir vergleichen die Context-Instanz aus dem Mapping mit unserem owningUc.
        de.bund.zrb.service.UserContextMappingService ucm = de.bund.zrb.service.UserContextMappingService.getInstance();
        for (de.bund.zrb.service.UserRegistry.User u : de.bund.zrb.service.UserRegistry.getInstance().getAll()) {
            com.microsoft.playwright.BrowserContext ctx = ucm.getContextForUser(u.getUsername());
            if (ctx == owningUc) { // identische Instanz
                return u;
            }
        }

        // Nichts gefunden → Fallback
        return de.bund.zrb.service.UserContextMappingService.getInstance().getCurrentUser();
    }

    /** Ermittelt den erwarteten Executable-Pfad für den gewählten Browser anhand der System-Konfiguration. */
    private String resolveExecutablePath(String browserType) {
        if (browserType == null) return null;
        switch (browserType.toLowerCase()) {
            case "firefox": return de.bund.zrb.config.BrowserSystemConfig.getFirefoxPath();
            case "chromium": return de.bund.zrb.config.BrowserSystemConfig.getChromiumPath();
            case "edge": return de.bund.zrb.config.BrowserSystemConfig.getEdgePath();
            default: return null;
        }
    }

    /** Prüft, ob eine Instanz läuft und beendet sie ggf. nach Rückfrage. Liefert false bei Abbruch. */
    private boolean detectAndOptionallyTerminateRunningBrowser(BrowserConfig config) {
        String exePath = resolveExecutablePath(config.getBrowserType());
        if (exePath == null || exePath.trim().isEmpty()) {
            return true; // nichts prüfbar
        }
        BrowserInstanceState state = browserProcessService.detectBrowserInstanceState(exePath);
        if (state != BrowserInstanceState.RUNNING) {
            return true; // nichts zu tun
        }
        Boolean confirmSetting = SettingsService.getInstance().get("browser.confirmTerminateRunning", Boolean.class);
        boolean askUser = confirmSetting == null ? true : confirmSetting;
        boolean doTerminate = askUser;
        if (askUser) {
            int choice = JOptionPane.showConfirmDialog(null,
                    "Es läuft bereits eine Instanz von '" + config.getBrowserType() + "'.\n" +
                            "Alle Instanzen beenden und neu starten?",
                    "Laufende Instanz erkannt",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                ApplicationEventBus.getInstance().publish(new BrowserLifecycleEvent(new BrowserLifecycleEvent.Payload(BrowserLifecycleEvent.Kind.ERROR, "Start abgebrochen (Instanz läuft weiter)")));
                return false; // Abbruch
            }
        }
        if (doTerminate) {
            BrowserTerminationResult tr = browserProcessService.terminateBrowserInstances(exePath);
            if (tr.isDetectionFailed()) {
                ApplicationEventBus.getInstance().publish(new BrowserLifecycleEvent(new BrowserLifecycleEvent.Payload(BrowserLifecycleEvent.Kind.ERROR, "⚠ Prozess-Erkennung fehlgeschlagen – versuche trotzdem zu starten")));
            } else if (tr.hasAnyFailure()) {
                ApplicationEventBus.getInstance().publish(new BrowserLifecycleEvent(new BrowserLifecycleEvent.Payload(BrowserLifecycleEvent.Kind.ERROR, "⚠ Einige Prozesse konnten nicht beendet werden")));
            } else if (tr.hasAnyTermination()) {
                ApplicationEventBus.getInstance().publish(new BrowserLifecycleEvent(new BrowserLifecycleEvent.Payload(BrowserLifecycleEvent.Kind.STOPPED, "Alte Instanzen beendet: " + tr.getTerminatedCount())));
            }
            // Kleines Delay, damit OS Freigabe des Ports vollzieht
            try { Thread.sleep(300); } catch (InterruptedException ignore) {}
        }
        return true;
    }

    /** Debug: entfernt ALLE existierenden UserContexts remote (sofern möglich) und lokale Mappings. */
    public void closeAllUserContexts() {
        if (browser == null) {
            userContexts.clear();
            return;
        }
        try {
            de.bund.zrb.manager.WDBrowserManager mgr = new de.bund.zrb.manager.WDBrowserManager(browser.getWebDriver().getWebSocketManager());
            de.bund.zrb.command.response.WDBrowserResult.GetUserContextsResult res = mgr.getUserContexts();
            res.getUserContexts().forEach(uc -> {
                String id = uc.getUserContext().value();
                if (!"default".equalsIgnoreCase(id)) {
                    try { mgr.removeUserContext(id); } catch (Throwable ignore) {}
                }
            });
        } catch (Throwable ignore) {
            // Fallback: lokale Kontexte schließen
            userContexts.values().forEach(c -> { try { c.close(); } catch (Throwable ignore2) {} });
        } finally {
            userContexts.clear();
        }
    }

}
