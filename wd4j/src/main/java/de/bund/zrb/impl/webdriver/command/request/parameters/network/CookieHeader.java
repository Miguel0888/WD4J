package de.bund.zrb.impl.webdriver.command.request.parameters.network;

import de.bund.zrb.impl.webdriver.type.network.WDCookie;

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
