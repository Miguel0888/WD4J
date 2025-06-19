package de.bund.zrb.impl.webdriver.command.request.parameters.network;

import de.bund.zrb.impl.webdriver.type.network.WDBytesValue;
import de.bund.zrb.impl.webdriver.type.network.WDHeader;
import de.bund.zrb.impl.webdriver.type.network.WDRequest;
import de.bund.zrb.impl.websocket.WDCommand;

import java.util.List;

public class ContinueRequestParameters implements WDCommand.Params {
    private final WDRequest WDRequest;
    private final WDBytesValue body; // Optional
    private final List<CookieHeader> cookies; // Optional
    private final List<WDHeader> WDHeaders; // Optional
    private final String method; // Optional
    private final String url;

    public ContinueRequestParameters(WDRequest WDRequest) {
        this(WDRequest, null, null, null, null, null);
    }

    public ContinueRequestParameters(WDRequest WDRequest, WDBytesValue body, List<CookieHeader> cookies, List<WDHeader> WDHeaders, String method, String url) {
        this.WDRequest = WDRequest;
        this.body = body;
        this.cookies = cookies;
        this.WDHeaders = WDHeaders;
        this.method = method;
        this.url = url;
    }

    public WDRequest getRequest() {
        return WDRequest;
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
