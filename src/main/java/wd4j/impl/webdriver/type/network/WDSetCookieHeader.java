package wd4j.impl.webdriver.type.network;

public class WDSetCookieHeader {
    private final String name;
    private final WDBytesValue value;
    private final String domain; // optional
    private final Boolean httpOnly; // optional
    private final String expiry; // optional
    private final Long maxAge; // optional
    private final String path; // optional
    private final WDSameSite WDSameSite; // optional
    private final Boolean secure; // optional

    public WDSetCookieHeader(String name, WDBytesValue value) {
        this.name = name;
        this.value = value;
        this.domain = null;
        this.httpOnly = null;
        this.expiry = null;
        this.maxAge = null;
        this.path = null;
        this.WDSameSite = null;
        this.secure = false;
    }

    public WDSetCookieHeader(String name, WDBytesValue value, String domain, boolean httpOnly, String expiry, long maxAge, String path, WDSameSite WDSameSite, boolean secure) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.httpOnly = httpOnly;
        this.expiry = expiry;
        this.maxAge = maxAge;
        this.path = path;
        this.WDSameSite = WDSameSite;
        this.secure = secure;
    }

    public String getName() {
        return name;
    }

    public WDBytesValue getValue() {
        return value;
    }

    public String getDomain() {
        return domain;
    }

    public Boolean getHttpOnly() {
        return httpOnly;
    }

    public String getExpiry() {
        return expiry;
    }

    public Long getMaxAge() {
        return maxAge;
    }

    public String getPath() {
        return path;
    }

    public WDSameSite getSameSite() {
        return WDSameSite;
    }

    public Boolean getSecure() {
        return secure;
    }
}
