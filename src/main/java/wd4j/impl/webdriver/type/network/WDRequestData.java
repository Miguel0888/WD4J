package wd4j.impl.webdriver.type.network;

import java.util.List;

public class WDRequestData {
    private final WDRequest WDRequest;
    private final String url;
    private final String method;
    private final List<WDHeader> WDHeaders;
    private final List<WDCookie> WDCookies;
    private final char headersSize;
    private final WDCookie bodySize;
    private final String destination;
    private final String initiatorType;
    private final WDFetchTimingInfo timings;

    public WDRequestData(WDRequest WDRequest, String url, String method, List<WDHeader> WDHeaders, List<WDCookie> WDCookies, char headersSize, WDCookie bodySize, String destination, String initiatorType, WDFetchTimingInfo timings) {
        this.WDRequest = WDRequest;
        this.url = url;
        this.method = method;
        this.WDHeaders = WDHeaders;
        this.WDCookies = WDCookies;
        this.headersSize = headersSize;
        this.bodySize = bodySize;
        this.destination = destination;
        this.initiatorType = initiatorType;
        this.timings = timings;
    }

    public WDRequest getRequest() {
        return WDRequest;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public List<WDHeader> getHeaders() {
        return WDHeaders;
    }

    public List<WDCookie> getCookies() {
        return WDCookies;
    }

    public char getHeadersSize() {
        return headersSize;
    }

    public WDCookie getBodySize() {
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