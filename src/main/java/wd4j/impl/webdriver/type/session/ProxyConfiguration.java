package wd4j.impl.webdriver.type.session;

import java.util.List;

public interface ProxyConfiguration {
    String getProxyType();
}

class AutodetectProxyConfiguration implements ProxyConfiguration {
    private final String proxyType = "autodetect";

    @Override
    public String getProxyType() {
        return proxyType;
    }
}

class DirectProxyConfiguration implements ProxyConfiguration {
    private final String proxyType = "direct";

    @Override
    public String getProxyType() {
        return proxyType;
    }
}

class ManualProxyConfiguration implements ProxyConfiguration {
    private final String proxyType = "manual";
    private final String ftpProxy; // Optional
    private final String httpProxy; // Optional
    private final String sslProxy; // Optional
    private final SocksProxyConfiguration socksProxyConfiguration; // Optional
    private final List<String> noProxy; // Optional

    public ManualProxyConfiguration(String ftpProxy, String httpProxy, String sslProxy,
                                    SocksProxyConfiguration socksProxyConfiguration, List<String> noProxy) {
        this.ftpProxy = ftpProxy;
        this.httpProxy = httpProxy;
        this.sslProxy = sslProxy;
        this.socksProxyConfiguration = socksProxyConfiguration;
        this.noProxy = noProxy;
    }

    @Override
    public String getProxyType() {
        return proxyType;
    }

    public String getFtpProxy() {
        return ftpProxy;
    }

    public String getHttpProxy() {
        return httpProxy;
    }

    public String getSslProxy() {
        return sslProxy;
    }

    public SocksProxyConfiguration getSocksProxyConfiguration() {
        return socksProxyConfiguration;
    }

    public List<String> getNoProxy() {
        return noProxy;
    }
}

class SocksProxyConfiguration {
    private final String socksProxy;
    private final byte socksVersion;

    public SocksProxyConfiguration(String socksProxy, byte socksVersion) {
        this.socksProxy = socksProxy;
        this.socksVersion = socksVersion;
    }

    public String getSocksProxy() {
        return socksProxy;
    }

    public int getSocksVersion() {
        return socksVersion;
    }
}

class PacProxyConfiguration implements ProxyConfiguration {
    private final String proxyType = "pac";
    private final String proxyAutoconfigUrl;

    public PacProxyConfiguration(String proxyAutoconfigUrl) {
        this.proxyAutoconfigUrl = proxyAutoconfigUrl;
    }

    @Override
    public String getProxyType() {
        return proxyType;
    }

    public String getProxyAutoconfigUrl() {
        return proxyAutoconfigUrl;
    }
}

class SystemProxyConfiguration implements ProxyConfiguration {
    private final String proxyType = "system";

    @Override
    public String getProxyType() {
        return proxyType;
    }
}