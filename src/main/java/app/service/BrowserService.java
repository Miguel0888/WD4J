package app.service;

import wd4j.core.WebDriver;
import wd4j.helper.BrowserType;

public class BrowserService {
    private WebDriver webDriver;

    public void createWebDriver(String browserName, int port, String profilePath, boolean headless, boolean disableGpu, boolean noRemote) {
        try {
            BrowserType browserType = BrowserType.valueOf(browserName.toUpperCase());
            browserType.setPort(port);
            browserType.setProfilePath(profilePath);
            browserType.setHeadless(headless);
            browserType.setDisableGpu(disableGpu);
            browserType.setNoRemote(noRemote);

            this.webDriver = new WebDriver(browserType);
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Erstellen des WebDrivers", e);
        }
    }

    public void navigateTo(String url) {
        if (webDriver != null) {
            webDriver.get(url);
        } else {
            throw new IllegalStateException("WebDriver ist nicht gestartet.");
        }
    }

}
