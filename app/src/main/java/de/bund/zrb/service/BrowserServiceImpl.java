package de.bund.zrb.service;

import com.microsoft.playwright.*;
import de.bund.zrb.*;
import de.bund.zrb.command.response.WDScriptResult;
import de.bund.zrb.manager.WDScriptManager;
import de.bund.zrb.type.script.WDLocalValue;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;
import de.bund.zrb.type.script.WDRealmInfo;
import de.bund.zrb.type.script.WDTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BrowserServiceImpl implements BrowserService {

    private final Map<String, BrowserContext> userContexts = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(BrowserServiceImpl.class);
    private static final BrowserServiceImpl INSTANCE = new BrowserServiceImpl();

    private Playwright playwright;
    private BrowserImpl browser;

    private final List<ActivePageListener> activePageListeners = new ArrayList<>();

    private BrowserServiceImpl() {}

    public static BrowserServiceImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public void launchBrowser(BrowserConfig config) {
        try {
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
            BrowserTypeImpl browserType = BrowserTypeImpl.newFirefoxInstance((PlaywrightImpl) playwright);
            browser = (BrowserImpl) browserType.launch(options);
        } catch (Exception ex) {
            throw       new RuntimeException("Fehler beim Starten des Browsers", ex);
        }
    }

    @Override
    public void terminateBrowser() {
        if (browser != null) {
            browser.close();
            playwright.close();
            browser = null;
            playwright = null;
        }
    }

    @Override
    public void navigate(String url) {
        browser.getPages().getActivePage().navigate(url);
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
    public Page getActivePage(String username) {
        BrowserContext context = getOrCreateUserContext(username);
        List<Page> pages = context.pages();
        if (pages.isEmpty()) {
            return context.newPage();
        }
        return pages.get(0);
    }

    @Override
    public Page createNewTab(String username) {
        BrowserContext context = getOrCreateUserContext(username);
        return context.newPage();
    }

    @Override
    public void closeActiveTab() {
        PageImpl activePage = browser.getPages().getActivePage();
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
        browser.getPages().getActivePage().goBack();
    }

    @Override
    public void goBack(String username) {
        getActivePage(username).goBack();
    }

    @Override
    public void goForward() {
        browser.getPages().getActivePage().goForward();
    }

    @Override
    public void goForward(String username) {
        getActivePage(username).goForward();
    }

    @Override
    public void reload() {
        browser.getPages().getActivePage().reload();
    }

    @Override
    public void reload(String username) {
        getActivePage(username).reload();
    }

    @Override
    public byte[] captureScreenshot() {
        return browser.getPages().getActivePage().screenshot();
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
        if (!Objects.equals(newContextId, browser.getPages().getActivePageId())) {
            browser.getPages().setActivePageId(newContextId, true);
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

    public interface ActivePageListener {
        void onActivePageChanged(String contextId);
    }
}
