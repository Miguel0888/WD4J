package wd4j.impl.webdriver.command.request.parameters.network;

import wd4j.impl.webdriver.type.network.WDAuthCredentials;
import wd4j.impl.webdriver.type.network.WDHeader;
import wd4j.impl.webdriver.type.network.WDRequest;
import wd4j.impl.webdriver.type.network.WDSetCookieHeader;
import wd4j.impl.websocket.WDCommand;

import java.util.List;

public class ContinueResponseParameters implements WDCommand.Params {
    private final WDRequest WDRequest;
    private final List<WDSetCookieHeader> cookies; // Optional
    private final WDAuthCredentials rawResponse; // Optional
    private final List<WDHeader> responseWDHeaders; // Optional
    private final String text; // Optional
    private final Integer statusCode; // Optional

    public ContinueResponseParameters(WDRequest WDRequest) {
        this(WDRequest, null, null, null, null, null);
    }

    public ContinueResponseParameters(WDRequest WDRequest, List<WDSetCookieHeader> cookies, WDAuthCredentials rawResponse, List<WDHeader> responseWDHeaders, String text, Integer statusCode) {
        this.WDRequest = WDRequest;
        this.cookies = cookies;
        this.rawResponse = rawResponse;
        this.responseWDHeaders = responseWDHeaders;
        this.text = text;
        this.statusCode = statusCode;
    }

    public WDRequest getRequest() {
        return WDRequest;
    }

    public List<WDSetCookieHeader> getCookies() {
        return cookies;
    }

    public WDAuthCredentials getRawResponse() {
        return rawResponse;
    }

    public List<WDHeader> getResponseHeaders() {
        return responseWDHeaders;
    }

    public String getText() {
        return text;
    }
}
