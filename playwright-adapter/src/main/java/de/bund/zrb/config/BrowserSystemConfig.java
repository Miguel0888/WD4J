package de.bund.zrb.config;

/**
 * Zentrale System-Config für Browser-Pfade, Default-WebSocket und Profile.
 *
 * Werte können über System Properties überschrieben werden:
 *  - browser.defaultUrl (z.B. ws://127.0.0.1)
 *  - browser.defaultPort (z.B. 9222)
 *  - browser.wsEndpoint (z.B. /session)
 *  - browser.firefox.path
 *  - browser.chromium.path
 *  - browser.edge.path
 *  - browser.profile.firefox
 *  - browser.profile.chromium
 *  - browser.profile.edge
 */
public final class BrowserSystemConfig {

    private static volatile String defaultUrl  = "ws://127.0.0.1";
    private static volatile int    defaultPort = 9222;
    private static volatile String wsEndpoint  = "/session";

    private static volatile String firefoxPath  = "C:\\Program Files\\Mozilla Firefox\\firefox.exe";
    private static volatile String chromiumPath = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
    private static volatile String edgePath     = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";

    private static volatile String firefoxProfile  = "C:\\FirefoxProfile";
    private static volatile String chromiumProfile = "C:\\ChromeProfile";
    private static volatile String edgeProfile     = "C:\\EdgeProfile";

    private BrowserSystemConfig() {}

    public static void initFromSystemProperties() {
        String v;
        if ((v = System.getProperty("browser.defaultUrl")) != null && !v.trim().isEmpty()) defaultUrl = v.trim();
        if ((v = System.getProperty("browser.defaultPort")) != null) try { defaultPort = Integer.parseInt(v.trim()); } catch(Exception ignored){}
        if ((v = System.getProperty("browser.wsEndpoint")) != null && !v.trim().isEmpty()) wsEndpoint = v.trim();

        if ((v = System.getProperty("browser.firefox.path")) != null && !v.trim().isEmpty()) firefoxPath = v.trim();
        if ((v = System.getProperty("browser.chromium.path")) != null && !v.trim().isEmpty()) chromiumPath = v.trim();
        if ((v = System.getProperty("browser.edge.path")) != null && !v.trim().isEmpty()) edgePath = v.trim();

        if ((v = System.getProperty("browser.profile.firefox")) != null && !v.trim().isEmpty()) firefoxProfile = v.trim();
        if ((v = System.getProperty("browser.profile.chromium")) != null && !v.trim().isEmpty()) chromiumProfile = v.trim();
        if ((v = System.getProperty("browser.profile.edge")) != null && !v.trim().isEmpty()) edgeProfile = v.trim();
    }

    public static String getDefaultUrl() { return defaultUrl; }
    public static int getDefaultPort() { return defaultPort; }
    public static String getWsEndpoint() { return wsEndpoint; }

    public static String getFirefoxPath() { return firefoxPath; }
    public static String getChromiumPath() { return chromiumPath; }
    public static String getEdgePath() { return edgePath; }

    public static String getFirefoxProfile() { return firefoxProfile; }
    public static String getChromiumProfile() { return chromiumProfile; }
    public static String getEdgeProfile() { return edgeProfile; }
}

