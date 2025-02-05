package wd4j.impl.webdriver.type.session;

public class ProxyConfiguration {
    private final String proxyType;
    private final String proxy;

    public ProxyConfiguration(String proxyType, String proxy) {
        if (proxyType == null || proxyType.isEmpty()) {
            throw new IllegalArgumentException("Proxy type must not be null or empty.");
        }
        this.proxyType = proxyType;
        this.proxy = proxy;
    }

    public String getProxyType() {
        return proxyType;
    }

    public String getProxy() {
        return proxy;
    }
}