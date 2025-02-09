package wd4j.impl.webdriver.command.request.network.parameters;

import wd4j.impl.webdriver.type.network.AuthCredentials;
import wd4j.impl.webdriver.type.network.Header;
import wd4j.impl.webdriver.type.network.Request;
import wd4j.impl.webdriver.type.network.SetCookieHeader;
import wd4j.impl.websocket.Command;

import java.util.List;

public class ContinueResponseParameters implements Command.Params {
    private final Request request;
    private final List<SetCookieHeader> cookies;
    private final AuthCredentials rawResponse;
    private final List<Header> responseHeaders;
    private final String text;
    private final char statusCode;

    public ContinueResponseParameters(Request request, List<SetCookieHeader> cookies, AuthCredentials rawResponse, List<Header> responseHeaders, String text, char statusCode) {
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
