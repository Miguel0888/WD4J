package wd4j.impl.webdriver.command.request.storage.parameters;

import wd4j.impl.webdriver.type.network.BytesValue;
import wd4j.impl.webdriver.type.network.SameSite;
import wd4j.impl.websocket.Command;

public class CookieFilter implements Command.Params {
    private final String name;
    private final BytesValue value;
    private final String domain;
    private final String path;
    private final int size;
    private final boolean httpOnly;
    private final boolean secure;
    private final SameSite sameSite;
    private final int expiry;

    public CookieFilter(String name, BytesValue value, String domain, String path, int size, boolean httpOnly, boolean secure, SameSite sameSite, int expiry) {
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

    public BytesValue getValue() {
        return value;
    }

    public String getDomain() {
        return domain;
    }

    public String getPath() {
        return path;
    }

    public int getSize() {
        return size;
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
