package de.bund.zrb.event;

import com.microsoft.Frame;
import com.microsoft.Request;
import com.microsoft.Response;
import com.microsoft.options.HttpHeader;
import com.microsoft.options.Sizes;
import com.microsoft.options.Timing;
import de.bund.zrb.event.WDNetworkEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RequestImpl implements Request {
    public RequestImpl(WDNetworkEvent.BeforeRequestSent beforeRequestSent) {
        // TODO: Implement this
    }

    public RequestImpl(WDNetworkEvent.AuthRequired authRequired) {
        // TODO: Implement this
    }

    @Override
    public Map<String, String> allHeaders() {
        return Collections.emptyMap();
    }

    @Override
    public String failure() {
        return "";
    }

    @Override
    public Frame frame() {
        return null;
    }

    @Override
    public Map<String, String> headers() {
        return Collections.emptyMap();
    }

    @Override
    public List<HttpHeader> headersArray() {
        return Collections.emptyList();
    }

    @Override
    public String headerValue(String name) {
        return "";
    }

    @Override
    public boolean isNavigationRequest() {
        return false;
    }

    @Override
    public String method() {
        return "";
    }

    @Override
    public String postData() {
        return "";
    }

    @Override
    public byte[] postDataBuffer() {
        return new byte[0];
    }

    @Override
    public Request redirectedFrom() {
        return null;
    }

    @Override
    public Request redirectedTo() {
        return null;
    }

    @Override
    public String resourceType() {
        return "";
    }

    @Override
    public Response response() {
        return null;
    }

    @Override
    public Sizes sizes() {
        return null;
    }

    @Override
    public Timing timing() {
        return null;
    }

    @Override
    public String url() {
        return "";
    }
}
