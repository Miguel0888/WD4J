package de.bund.zrb.webdriver.command.request.parameters.network;

import de.bund.zrb.webdriver.type.network.WDBytesValue;
import de.bund.zrb.webdriver.type.network.WDHeader;
import de.bund.zrb.webdriver.type.network.WDRequest;
import de.bund.zrb.webdriver.type.network.WDSetCookieHeader;
import de.bund.zrb.websocket.WDCommand;

import java.util.List;

public class ProvideResponseParameters implements WDCommand.Params {
    private final WDRequest WDRequestId;
    private final WDBytesValue body; // Optional
    private final List<WDSetCookieHeader> cookies; // Optional
    private final List<WDHeader> WDHeaders; // Optional
    private final String reasonPhrase; // Optional
    private final Integer statusCode; // Optional

    public ProvideResponseParameters(WDRequest WDRequestId) {
        this(WDRequestId, null, null, null, null, null);
    }

    public ProvideResponseParameters(WDRequest WDRequestId, WDBytesValue body, List<WDSetCookieHeader> cookies, List<WDHeader> WDHeaders, String reasonPhrase, Integer statusCode) {
        this.WDRequestId = WDRequestId;
        this.body = body;
        this.cookies = cookies;
        this.WDHeaders = WDHeaders;
        this.reasonPhrase = reasonPhrase;
        this.statusCode = statusCode;
    }

    public WDRequest getRequestId() {
        return WDRequestId;
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
