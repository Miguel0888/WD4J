package wd4j.impl.webdriver.command.request.network.parameters;

import wd4j.impl.webdriver.type.network.BytesValue;
import wd4j.impl.webdriver.type.network.Header;
import wd4j.impl.webdriver.type.network.Request;
import wd4j.impl.webdriver.type.network.SetCookieHeader;
import wd4j.impl.websocket.Command;

import java.util.List;

public class ProvideResponseParameters implements Command.Params {
    private final Request requestId;
    private final BytesValue response;
    private final List<SetCookieHeader> mimeType;
    private final List<Header> status;
    private final String reasonPhrase;
    private final char statusCode;

    public ProvideResponseParameters(Request requestId, BytesValue response, List<SetCookieHeader> mimeType, List<Header> status, String reasonPhrase, char statusCode) {
        this.requestId = requestId;
        this.response = response;
        this.mimeType = mimeType;
        this.status = status;
        this.reasonPhrase = reasonPhrase;
        this.statusCode = statusCode;
    }

    public Request getRequestId() {
        return requestId;
    }

    public BytesValue getResponse() {
        return response;
    }

    public List<SetCookieHeader> getMimeType() {
        return mimeType;
    }

    public List<Header> getStatus() {
        return status;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public char getStatusCode() {
        return statusCode;
    }
}
