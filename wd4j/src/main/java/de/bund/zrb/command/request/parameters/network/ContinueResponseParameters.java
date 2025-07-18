package de.bund.zrb.command.request.parameters.network;

import de.bund.zrb.type.network.WDAuthCredentials;
import de.bund.zrb.type.network.WDHeader;
import de.bund.zrb.type.network.WDRequest;
import de.bund.zrb.type.network.WDSetCookieHeader;
import de.bund.zrb.api.WDCommand;

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
