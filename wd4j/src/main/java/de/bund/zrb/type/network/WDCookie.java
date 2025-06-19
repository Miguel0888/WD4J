package de.bund.zrb.type.network;

public class WDCookie {
    private final String name;
    private final WDBytesValue value;
    private final String domain;
    private final String path;
    private final long size;
    private final boolean httpOnly;
    private final boolean secure;
    private final WDSameSite sameSite;
    private final Long expiry; // optional

    public WDCookie(String name, WDBytesValue value, String domain, String path, long size, boolean httpOnly, boolean secure, WDSameSite sameSite) {
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

    public WDCookie(String name, WDBytesValue value, String domain, String path, Long size, boolean httpOnly, boolean secure, WDSameSite sameSite, Long expiry) {
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

    public long getSize() {
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

    public Long getExpiry() {
        return expiry;
    }
}