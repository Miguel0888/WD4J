package wd4j.impl.webdriver.command.request.parameters.storage;

import wd4j.impl.webdriver.type.network.BytesValue;
import wd4j.impl.webdriver.type.network.SameSite;
import wd4j.impl.websocket.Command;

public class CookieFilter implements Command.Params {
    private final String name; // Optional
    private final BytesValue value; // Optional
    private final String domain; // Optional
    private final String path; // Optional
    private final Integer size; // Optional
    private final Boolean httpOnly; // Optional
    private final Boolean secure; // Optional
    private final SameSite sameSite; // Optional
    private final Integer expiry; // Optional

    public CookieFilter() {
        this(null, null, null, null, null, null, null, null, null);
    }

    public CookieFilter(String name, BytesValue value, String domain, String path, Integer size, Boolean httpOnly, Boolean secure, SameSite sameSite, Integer expiry) {
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

    public Integer getSize() {
        return size;
    }

    public Boolean isHttpOnly() {
        return httpOnly;
    }

    public Boolean isSecure() {
        return secure;
    }

    public SameSite getSameSite() {
        return sameSite;
    }

    public Integer getExpiry() {
        return expiry;
    }
}
