package wd4j.impl.webdriver.command.request.parameters.network;

import wd4j.impl.webdriver.type.network.BytesValue;
import wd4j.impl.webdriver.type.network.Header;
import wd4j.impl.webdriver.type.network.Request;
import wd4j.impl.webdriver.type.network.SetCookieHeader;
import wd4j.impl.websocket.Command;

import java.util.List;

public class ProvideResponseParameters implements Command.Params {
    private final Request requestId;
    private final BytesValue body; // Optional
    private final List<SetCookieHeader> cookies; // Optional
    private final List<Header> headers; // Optional
    private final String reasonPhrase; // Optional
    private final Integer statusCode; // Optional

    public ProvideResponseParameters(Request requestId) {
        this(requestId, null, null, null, null, null);
    }

    public ProvideResponseParameters(Request requestId, BytesValue body, List<SetCookieHeader> cookies, List<Header> headers, String reasonPhrase, Integer statusCode) {
        this.requestId = requestId;
        this.body = body;
        this.cookies = cookies;
        this.headers = headers;
        this.reasonPhrase = reasonPhrase;
        this.statusCode = statusCode;
    }

    public Request getRequestId() {
        return requestId;
    }

    public BytesValue getBody() {
        return body;
    }

    public List<SetCookieHeader> getCookies() {
        return cookies;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
