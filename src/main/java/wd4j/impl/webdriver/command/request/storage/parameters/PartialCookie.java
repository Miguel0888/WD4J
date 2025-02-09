package wd4j.impl.webdriver.command.request.storage.parameters;

import wd4j.impl.webdriver.type.network.BytesValue;
import wd4j.impl.webdriver.type.network.SameSite;

public class PartialCookie {

    private String name;
    private BytesValue value;
    private String domain;
    private String path;
    private boolean httpOnly;
    private boolean secure;
    private SameSite sameSite;
    private int expiry;

    public PartialCookie(String name, BytesValue value, String domain, String path, boolean httpOnly, boolean secure, SameSite sameSite, int expiry) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
        this.httpOnly = httpOnly;
        this.secure = secure;
        this.sameSite = sameSite;
        this.expiry = expiry;
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

    public String getPath() {
        return path;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public boolean isSecure() {
        return secure;
    }

    public SameSite getSameSite() {
        return sameSite;
    }

    public int getExpiry() {
        return expiry;
    }
}
