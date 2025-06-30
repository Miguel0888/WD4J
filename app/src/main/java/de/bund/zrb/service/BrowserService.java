package de.bund.zrb.service;

import java.util.List;

public interface BrowserService {
    void launchBrowser(BrowserConfig config);

    void terminateBrowser();

    void navigate(String url);

    void createNewTab();

    void closeActiveTab();

    void goForward();

    void reload();

    void goBack();

    byte[] captureScreenshot();

    void showSelectors(boolean enabled);

    void showDomEvents(boolean enabled);
}
