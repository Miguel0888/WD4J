package wd4j.impl.webdriver.type.network;

public class WDCookie {
    private final String name;
    private final WDBytesValue value;
    private final String domain;
    private final String path;
    private final char size;
    private final boolean httpOnly;
    private final boolean secure;
    private final WDSameSite sameSite;
    private final Character expiry; // optional

    public WDCookie(String name, WDBytesValue value, String domain, String path, char size, boolean httpOnly, boolean secure, WDSameSite sameSite) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
        this.size = size;
        this.httpOnly = httpOnly;
        this.secure = secure;
        this.sameSite = sameSite;
        this.expiry = null;
    }

    public WDCookie(String name, WDBytesValue value, String domain, String path, char size, boolean httpOnly, boolean secure, WDSameSite sameSite, char expiry) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
        this.size = size;
        this.httpOnly = httpOnly;
        this.secure = secure;
        this.sameSite = sameSite;
        this.expiry = expiry;
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

    public String getPath() {
        return path;
    }

    public char getSize() {
        return size;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public boolean isSecure() {
        return secure;
    }

    public WDSameSite getSameSite() {
        return sameSite;
    }

    public Character getExpiry() {
        return expiry;
    }
}