package de.bund.zrb.webdriver.type.network;

import java.util.List;

public class WDRequestData {
    private final WDRequest request;
    private final String url;
    private final String method;
    private final List<WDHeader> headers;
    private final List<WDCookie> cookies;
    private final long headersSize;
    private final long bodySize;
    private final String destination;
    private final String initiatorType;
    private final WDFetchTimingInfo timings;

    public WDRequestData(WDRequest request, String url, String method, List<WDHeader> headers, List<WDCookie> cookies, long headersSize, long bodySize, String destination, String initiatorType, WDFetchTimingInfo timings) {
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

    public WDRequest getRequest() {
        return request;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public List<WDHeader> getHeaders() {
        return headers;
    }

    public List<WDCookie> getCookies() {
        return cookies;
    }

    public long getHeadersSize() {
        return headersSize;
    }

    public long getBodySize() {
        return bodySize;
    }

    public String getDestination() {
        return destination;
    }

    public String getInitiatorType() {
        return initiatorType;
    }

    public WDFetchTimingInfo getTimings() {
        return timings;
    }
}