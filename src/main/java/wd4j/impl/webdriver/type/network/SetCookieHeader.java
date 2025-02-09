package wd4j.impl.webdriver.type.network;

public class SetCookieHeader {
    private final String name;
    private final BytesValue value;
    private final String domain; // optional
    private final Boolean httpOnly; // optional
    private final String expiry; // optional
    private final Long maxAge; // optional
    private final String path; // optional
    private final SameSite sameSite; // optional
    private final Boolean secure; // optional

    public SetCookieHeader(String name, BytesValue value) {
        this.name = name;
        this.value = value;
        this.domain = null;
        this.httpOnly = null;
        this.expiry = null;
        this.maxAge = null;
        this.path = null;
        this.sameSite = null;
        this.secure = false;
    }

    public SetCookieHeader(String name, BytesValue value, String domain, boolean httpOnly, String expiry, long maxAge, String path, SameSite sameSite, boolean secure) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.httpOnly = httpOnly;
        this.expiry = expiry;
        this.maxAge = maxAge;
        this.path = path;
        this.sameSite = sameSite;
        this.secure = secure;
    }

    public String getName() {
        return name;
    }

    public BytesValue getValue() {
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

    public SameSite getSameSite() {
        return sameSite;
    }

    public Boolean getSecure() {
        return secure;
    }
}
