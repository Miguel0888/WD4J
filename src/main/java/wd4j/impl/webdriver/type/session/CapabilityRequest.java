package wd4j.impl.webdriver.type.session;

public class CapabilityRequest {
    private final Boolean acceptInsecureCerts; // Optional
    private final String browserName; // Optional
    private final String browserVersion; // Optional
    private final String platformName; // Optional
    private final ProxyConfiguration proxy; // Optional
    private final UserPromptHandler unhandledPromptBehavior; // Optional

    public CapabilityRequest(Boolean acceptInsecureCerts, String browserName, String browserVersion,
                             String platformName, ProxyConfiguration proxy, UserPromptHandler unhandledPromptBehavior) {
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

    public ProxyConfiguration getProxy() {
        return proxy;
    }

    public UserPromptHandler getUnhandledPromptBehavior() {
        return unhandledPromptBehavior;
    }
}