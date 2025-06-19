package de.bund.zrb.command.request.parameters.network;

import de.bund.zrb.type.network.WDCookie;

import java.util.List;

public class CookieHeader {
    private final List<WDCookie> WDCookies;

    public CookieHeader(List<WDCookie> WDCookies) {
        this.WDCookies = WDCookies;
    }

    public List<WDCookie> getCookies() {
        return WDCookies;
    }
}
