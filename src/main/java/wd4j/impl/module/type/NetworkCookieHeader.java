package wd4j.impl.module.type;

import java.util.List;

public class NetworkCookieHeader {
    private final List<NetworkCookie> cookies;

    public NetworkCookieHeader(List<NetworkCookie> cookies) {
        this.cookies = cookies;
    }

    public List<NetworkCookie> getCookies() {
        return cookies;
    }
}
