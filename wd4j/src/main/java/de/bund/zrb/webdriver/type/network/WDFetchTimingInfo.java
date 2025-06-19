package de.bund.zrb.webdriver.type.network;

public class WDFetchTimingInfo {
    private final float timeOrigin;
    private final float requestTime;
    private final float redirectStart;
    private final float redirectEnd;
    private final float fetchStart;
    private final float dnsStart;
    private final float dnsEnd;
    private final float connectStart;
    private final float connectEnd;
    private final float tlsStart;
    private final float requestStart;
    private final float responseStart;
    private final float responseEnd;

    public WDFetchTimingInfo(float timeOrigin, float requestTime, float redirectStart, float redirectEnd, float fetchStart, float dnsStart, float dnsEnd, float connectStart, float connectEnd, float tlsStart, float requestStart, float responseStart, float responseEnd) {
        this.timeOrigin = timeOrigin;
        this.requestTime = requestTime;
        this.redirectStart = redirectStart;
        this.redirectEnd = redirectEnd;
        this.fetchStart = fetchStart;
        this.dnsStart = dnsStart;
        this.dnsEnd = dnsEnd;
        this.connectStart = connectStart;
        this.connectEnd = connectEnd;
        this.tlsStart = tlsStart;
        this.requestStart = requestStart;
        this.responseStart = responseStart;
        this.responseEnd = responseEnd;
    }

    public float getTimeOrigin() {
        return timeOrigin;
    }

    public float getRequestTime() {
        return requestTime;
    }

    public float getRedirectStart() {
        return redirectStart;
    }

    public float getRedirectEnd() {
        return redirectEnd;
    }

    public float getFetchStart() {
        return fetchStart;
    }

    public float getDnsStart() {
        return dnsStart;
    }

    public float getDnsEnd() {
        return dnsEnd;
    }

    public float getConnectStart() {
        return connectStart;
    }

    public float getConnectEnd() {
        return connectEnd;
    }

    public float getTlsStart() {
        return tlsStart;
    }

    public float getRequestStart() {
        return requestStart;
    }

    public float getResponseStart() {
        return responseStart;
    }

    public float getResponseEnd() {
        return responseEnd;
    }
}