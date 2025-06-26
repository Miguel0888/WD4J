package de.bund.zrb.service;

import com.microsoft.playwright.*;
import de.bund.zrb.BrowserImpl;
import de.bund.zrb.PageImpl;
import de.bund.zrb.UserContextImpl;
import de.bund.zrb.command.response.WDScriptResult;
import de.bund.zrb.controller.CallbackWebSocketServer;
import de.bund.zrb.manager.WDScriptManager;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.browsingContext.WDLocator;
import de.bund.zrb.type.script.WDLocalValue;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;
import de.bund.zrb.type.script.WDRealmInfo;
import de.bund.zrb.type.script.WDTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.*;

public class BrowserServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(BrowserServiceImpl.class);
    private static final BrowserServiceImpl INSTANCE = new BrowserServiceImpl();

    private Playwright playwright;
    private BrowserImpl browser;

    private final List<ActivePageListener> activePageListeners = new ArrayList<>();

    private BrowserServiceImpl() {}

    public static BrowserServiceImpl getInstance() {
        return INSTANCE;
    }

    public void launchBrowser(BrowserConfig config) {
        try {
            playwright = Playwright.create();
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                    .setHeadless(config.isHeadless());

            List<String> args = new ArrayList<>();

            if (config.getPort() > 0) {
                args.add("--remote-debugging-port=" + config.getPort());
            }

            if (config.isNoRemote()) args.add("--no-remote");
            if (config.isDisableGpu()) args.add("--disable-gpu");
            if (config.isStartMaximized()) args.add("--start-maximized");

            if (config.isUseProfile()) {
                String profilePath = config.getProfilePath();
                if (profilePath == null || profilePath.trim().isEmpty()) {
                    profilePath = System.getProperty("java.io.tmpdir") + "temp_profile_" + System.currentTimeMillis();
                }

                if ("firefox".equalsIgnoreCase(config.getBrowserType())) {
                    args.add("--profile");
                    args.add(profilePath);
                } else {
                    args.add("--user-data-dir=" + profilePath);
                }
            }

            options.setArgs(args);

            if ("firefox".equalsIgnoreCase(config.getBrowserType())) {
                Map<String, Object> firefoxPrefs = new HashMap<>();
                firefoxPrefs.put("browser.startup.homepage", "https://www.google.com");
                options.setFirefoxUserPrefs(firefoxPrefs);
            }

            switch (config.getBrowserType().toLowerCase()) {
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
                    throw new IllegalArgumentException("Unsupported browser: " + config.getBrowserType());
            }

            CallbackWebSocketServer.toggleCallbackServer(true);
        } catch (Exception ex) {
            throw new RuntimeException("Fehler beim Starten des Browsers", ex);
        }
    }

    public void terminateBrowser() {
        if (browser != null) {
            browser.close();
            playwright.close();
            browser = null;
            playwright = null;
        }
    }

    public void navigate(String url) {
        browser.getPages().getActivePage().navigate(url);
    }

    public void createNewTab() {
        browser.newPage();
    }

    public void closeActiveTab() {
        PageImpl activePage = browser.getPages().getActivePage();
        if (activePage != null) {
            activePage.close();
        }
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

    public byte[] captureScreenshot() {
        return browser.getPages().getActivePage().screenshot();
    }

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

    public BrowserImpl getBrowser() {
        return browser;
    }

    public interface ActivePageListener {
        void onActivePageChanged(String contextId);
    }
}
