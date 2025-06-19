package de.bund.zrb.command.request.parameters.storage;

import de.bund.zrb.type.network.WDBytesValue;
import de.bund.zrb.type.network.WDSameSite;
import de.bund.zrb.api.WDCommand;

public class CookieFilter implements WDCommand.Params {
    private final String name; // Optional
    private final WDBytesValue value; // Optional
    private final String domain; // Optional
    private final String path; // Optional
    private final Integer size; // Optional
    private final Boolean httpOnly; // Optional
    private final Boolean secure; // Optional
    private final WDSameSite WDSameSite; // Optional
    private final Integer expiry; // Optional

    public CookieFilter() {
        this(null, null, null, null, null, null, null, null, null);
    }

    public CookieFilter(String name, WDBytesValue value, String domain, String path, Integer size, Boolean httpOnly, Boolean secure, WDSameSite WDSameSite, Integer expiry) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
        this.size = size;
        this.httpOnly = httpOnly;
        this.secure = secure;
        this.WDSameSite = WDSameSite;
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

    public Integer getSize() {
        return size;
    }

    public Boolean isHttpOnly() {
        return httpOnly;
    }

    public Boolean isSecure() {
        return secure;
    }

    public WDSameSite getSameSite() {
        return WDSameSite;
    }

    public Integer getExpiry() {
        return expiry;
    }
}
