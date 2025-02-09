package wd4j.impl.webdriver.command.request.parameters.network;

import wd4j.impl.webdriver.type.network.Cookie;

import java.util.List;

public class CookieHeader {
    private final List<Cookie> cookies;

    public CookieHeader(List<Cookie> cookies) {
        this.cookies = cookies;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }
}
