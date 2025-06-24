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

    private BrowserServiceImpl() {}

    public static BrowserServiceImpl getInstance() {
        return INSTANCE;
    }

    public void launchBrowser(String selectedBrowser, boolean headless, Map<String, Object> optionsMap) {
        try {
            playwright = Playwright.create();
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(headless);

            List<String> args = (List<String>) optionsMap.get("args");
            Map<String, Object> firefoxPrefs = (Map<String, Object>) optionsMap.get("firefoxPrefs");

            if (args != null) options.setArgs(args);
            if (firefoxPrefs != null) options.setFirefoxUserPrefs(firefoxPrefs);

            if ("chromium".equalsIgnoreCase(selectedBrowser)) {
                browser = (BrowserImpl) playwright.chromium().launch(options);
            } else if ("firefox".equalsIgnoreCase(selectedBrowser)) {
                browser = (BrowserImpl) playwright.firefox().launch(options);
            } else if ("webkit".equalsIgnoreCase(selectedBrowser)) {
                browser = (BrowserImpl) playwright.webkit().launch(options);
            } else {
                throw new IllegalArgumentException("Unsupported browser: " + selectedBrowser);
            }

            CallbackWebSocketServer.toggleCallbackServer(true); // required since we do not use WD Events
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
            List<WDLocalValue> args = new ArrayList<WDLocalValue>();
            args.add(new WDPrimitiveProtocolValue.BooleanValue(selected));

            scriptManager.callFunction("toggleTooltip", false,
                    new WDTarget.RealmTarget(realm.getRealm()), args);
        }
    }

    public void showDomEvents(boolean selected) {
        WDScriptManager scriptManager = browser.getWebDriver().script();
        WDScriptResult.GetRealmsResult realmsResult = scriptManager.getRealms();

        for (WDRealmInfo realm : realmsResult.getRealms()) {
            List<WDLocalValue> args = new ArrayList<WDLocalValue>();
            args.add(new WDPrimitiveProtocolValue.BooleanValue(selected));

            scriptManager.callFunction("toggleDomObserver", false,
                    new WDTarget.RealmTarget(realm.getRealm()), args);
        }
    }

    public BrowserImpl getBrowser() {
        return browser;
    }
}
