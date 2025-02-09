package wd4j.impl.webdriver.type.network;

import java.util.List;

public class RequestData {
    private final Request request;
    private final String url;
    private final String method;
    private final List<Header> headers;
    private final List<Cookie> cookies;
    private final char headersSize;
    private final Cookie bodySize;
    private final String destination;
    private final String initiatorType;
    private final FetchTimingInfo timings;

    public RequestData(Request request, String url, String method, List<Header> headers, List<Cookie> cookies, char headersSize, Cookie bodySize, String destination, String initiatorType, FetchTimingInfo timings) {
        this.request = request;
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.cookies = cookies;
        this.headersSize = headersSize;
        this.bodySize = bodySize;
        this.destination = destination;
        this.initiatorType = initiatorType;
        this.timings = timings;
    }

    public Request getRequest() {
        return request;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public char getHeadersSize() {
        return headersSize;
    }

    public Cookie getBodySize() {
        return bodySize;
    }

    public String getDestination() {
        return destination;
    }

    public String getInitiatorType() {
        return initiatorType;
    }

    public FetchTimingInfo getTimings() {
        return timings;
    }
}