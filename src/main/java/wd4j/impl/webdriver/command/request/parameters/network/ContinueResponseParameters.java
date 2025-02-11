package wd4j.impl.webdriver.command.request.parameters.network;

import wd4j.impl.webdriver.type.network.AuthCredentials;
import wd4j.impl.webdriver.type.network.Header;
import wd4j.impl.webdriver.type.network.Request;
import wd4j.impl.webdriver.type.network.SetCookieHeader;
import wd4j.impl.websocket.Command;

import java.util.List;

public class ContinueResponseParameters implements Command.Params {
    private final Request request;
    private final List<SetCookieHeader> cookies; // Optional
    private final AuthCredentials rawResponse; // Optional
    private final List<Header> responseHeaders; // Optional
    private final String text; // Optional
    private final Integer statusCode; // Optional

    public ContinueResponseParameters(Request request) {
        this(request, null, null, null, null, null);
    }

    public ContinueResponseParameters(Request request, List<SetCookieHeader> cookies, AuthCredentials rawResponse, List<Header> responseHeaders, String text, Integer statusCode) {
        this.request = request;
        this.cookies = cookies;
        this.rawResponse = rawResponse;
        this.responseHeaders = responseHeaders;
        this.text = text;
        this.statusCode = statusCode;
    }

    public Request getRequest() {
        return request;
    }

    public List<SetCookieHeader> getCookies() {
        return cookies;
    }

    public AuthCredentials getRawResponse() {
        return rawResponse;
    }

    public List<Header> getResponseHeaders() {
        return responseHeaders;
    }

    public String getText() {
        return text;
    }
}
