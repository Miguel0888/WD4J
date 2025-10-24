package de.bund.zrb.command.request.parameters.network;

import de.bund.zrb.type.network.WDBytesValue;
import de.bund.zrb.type.network.WDHeader;
import de.bund.zrb.type.network.WDRequest;
import de.bund.zrb.type.network.WDSetCookieHeader;
import de.bund.zrb.api.WDCommand;

import java.util.List;

public class ProvideResponseParameters implements WDCommand.Params {
    private final WDRequest request;
    private final WDBytesValue body; // Optional
    private final List<WDSetCookieHeader> cookies; // Optional
    private final List<WDHeader> WDHeaders; // Optional
    private final String reasonPhrase; // Optional
    private final Integer statusCode; // Optional

    public ProvideResponseParameters(WDRequest request) {
        this(request, null, null, null, null, null);
    }

    public ProvideResponseParameters(WDRequest request, WDBytesValue body, List<WDSetCookieHeader> cookies, List<WDHeader> headers, String reasonPhrase, Integer statusCode) {
        this.request = request;
        this.body = body;
        this.cookies = cookies;
        this.WDHeaders = headers;
        this.reasonPhrase = reasonPhrase;
        this.statusCode = statusCode;
    }

    public WDRequest getRequest() {
        return request;
    }

    public WDBytesValue getBody() {
        return body;
    }

    public List<WDSetCookieHeader> getCookies() {
        return cookies;
    }

    public List<WDHeader> getHeaders() {
        return WDHeaders;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
