package wd4j.impl.webdriver.command.request.parameters.network;

import wd4j.impl.webdriver.type.network.BytesValue;
import wd4j.impl.webdriver.type.network.Header;
import wd4j.impl.webdriver.type.network.Request;
import wd4j.impl.websocket.Command;

import java.util.List;

public class ContinueRequestParameters implements Command.Params {
    private final Request request;
    private final BytesValue body;
    private final List<CookieHeader> cookies;
    private final List<Header> headers;
    private final String method;
    private final String url;

    public ContinueRequestParameters(Request request, BytesValue body, List<CookieHeader> cookies, List<Header> headers, String method, String url) {
        this.request = request;
        this.body = body;
        this.cookies = cookies;
        this.headers = headers;
        this.method = method;
        this.url = url;
    }

    public Request getRequest() {
        return request;
    }

    public BytesValue getBody() {
        return body;
    }

    public List<CookieHeader> getCookies() {
        return cookies;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }
}
