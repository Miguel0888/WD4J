package de.bund.zrb.service;

import com.microsoft.playwright.Page;

import java.util.List;

public interface BrowserService {
    void launchBrowser(BrowserConfig config);

    void terminateBrowser();

    void navigate(String url);

    String createUserContext(UserRegistry.User user);

    void closeUserContext(String username);

    Page getActivePage(String username);

    void goBack();
    void goBack(String username);

    void goForward();
    void goForward(String username);

    void reload();
    void reload(String username);

    void closeActiveTab();
    void closeActiveTab(String username);

    void createNewTab();
    void createNewTab(String username);

    byte[] captureScreenshot();

    void showSelectors(boolean enabled);

    void showDomEvents(boolean enabled);
}
