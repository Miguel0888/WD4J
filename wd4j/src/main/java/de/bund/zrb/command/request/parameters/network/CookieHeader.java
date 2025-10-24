package de.bund.zrb.command.request.parameters.network;

import de.bund.zrb.type.network.WDCookie;

import java.util.List;

public class CookieHeader {
    private final List<WDCookie> cookies;

    public CookieHeader(List<WDCookie> cookies) {
        this.cookies = cookies;
    }

    public List<WDCookie> getCookies() {
        return cookies;
    }
}
