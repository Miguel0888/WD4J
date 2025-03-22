package wd4j.impl.dto.command.request.parameters.network;

import wd4j.impl.dto.type.network.WDCookie;

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
