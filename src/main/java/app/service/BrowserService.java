package app.service;

import wd4j.impl.BiDiWebDriver;
import wd4j.impl.BrowserType;
import wd4j.impl.WebSocketEndpointHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

public class BrowserService {
    private BiDiWebDriver webDriver;

    public void createWebDriver(String browserName, int port, String profilePath, boolean headless, boolean disableGpu, boolean noRemote) {
        try {
            BrowserType browserType = BrowserType.valueOf(browserName.toUpperCase());
            browserType.setPort(port);
            browserType.setProfilePath(profilePath);
            browserType.setHeadless(headless);
            browserType.setDisableGpu(disableGpu);
            browserType.setNoRemote(noRemote);

            this.webDriver = new BiDiWebDriver(browserType);
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

    public void terminateWebDriver() {
        if (webDriver != null) {
            webDriver.close();
            webDriver = null;
        } else {
            System.out.println("Kein WebDriver aktiv.");
        }
    }

}
