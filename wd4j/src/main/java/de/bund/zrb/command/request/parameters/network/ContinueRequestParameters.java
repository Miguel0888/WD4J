package de.bund.zrb.command.request.parameters.network;

import de.bund.zrb.type.network.WDBytesValue;
import de.bund.zrb.type.network.WDHeader;
import de.bund.zrb.type.network.WDRequest;
import de.bund.zrb.api.WDCommand;

import java.util.List;

public class ContinueRequestParameters implements WDCommand.Params {
    private final WDRequest request;
    private final WDBytesValue body; // Optional
    private final List<CookieHeader> cookies; // Optional
    private final List<WDHeader> WDHeaders; // Optional
    private final String method; // Optional
    private final String url;

    public ContinueRequestParameters(WDRequest request) {
        this(request, null, null, null, null, null);
    }

    public ContinueRequestParameters(WDRequest request, WDBytesValue body, List<CookieHeader> cookies, List<WDHeader> WDHeaders, String method, String url) {
        this.request = request;
        this.body = body;
        this.cookies = cookies;
        this.WDHeaders = WDHeaders;
        this.method = method;
        this.url = url;
    }

    public WDRequest getRequest() {
        return request;
    }

    public WDBytesValue getBody() {
        return body;
    }

    public List<CookieHeader> getCookies() {
        return cookies;
    }

    public List<WDHeader> getHeaders() {
        return WDHeaders;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }
}
