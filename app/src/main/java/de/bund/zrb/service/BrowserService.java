package de.bund.zrb.service;

import com.microsoft.playwright.Page;

import java.util.List;

public interface BrowserService {
    void launchBrowser(BrowserConfig config);

    void terminateBrowser();

    void navigate(String url);

    void createNewTab();

    String createUserContext(UserRegistry.User user);

    void closeUserContext(String username);

    Page getActivePage(String username);

    void closeActiveTab();

    void goForward();

    void reload();

    void goBack();

    byte[] captureScreenshot();

    void showSelectors(boolean enabled);

    void showDomEvents(boolean enabled);
}
