package wd4j.impl.webdriver.type.session;

/**
 * The session.CapabilityRequest type represents a specific set of requested capabilities.
 *
 * WebDriver BiDi defines additional WebDriver capabilities. The following tables enumerates the capabilities each implementation must support for WebDriver BiDi.
 *
 * <table>
 *   <tr>
 *     <th>Capability</th>
 *     <th>WebSocket URL</th>
 *   </tr>
 *   <tr>
 *     <td>Key</td>
 *     <td>"webSocketUrl"</td>
 *   </tr>
 *   <tr>
 *     <td>Value type</td>
 *     <td>boolean</td>
 *   </tr>
 *   <tr>
 *     <td>Description</td>
 *     <td>Defines the current session’s support for bidirectional connection.</td>
 *   </tr>
 * </table>
 */
public class WDCapabilityRequest {
    private final Boolean acceptInsecureCerts; // Optional
    private final String browserName; // Optional
    private final String browserVersion; // Optional
    private final String platformName; // Optional
    private final WDProxyConfiguration proxy; // Optional
    private final WDUserPromptHandler unhandledPromptBehavior; // Optional

    public WDCapabilityRequest() {
        this(null, null, null, null, null, null);
    }

    public WDCapabilityRequest(Boolean acceptInsecureCerts, String browserName, String browserVersion,
                               String platformName, WDProxyConfiguration proxy, WDUserPromptHandler unhandledPromptBehavior) {
        this.acceptInsecureCerts = acceptInsecureCerts;
        this.browserName = browserName;
        this.browserVersion = browserVersion;
        this.platformName = platformName;
        this.proxy = proxy;
        this.unhandledPromptBehavior = unhandledPromptBehavior;
    }

    public Boolean getAcceptInsecureCerts() {
        return acceptInsecureCerts;
    }

    public String getBrowserName() {
        return browserName;
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public String getPlatformName() {
        return platformName;
    }

    public WDProxyConfiguration getProxy() {
        return proxy;
    }

    public WDUserPromptHandler getUnhandledPromptBehavior() {
        return unhandledPromptBehavior;
    }
}