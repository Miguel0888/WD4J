package de.bund.zrb.event;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.HttpHeader;
import com.microsoft.playwright.options.Sizes;
import com.microsoft.playwright.options.Timing;
import de.bund.zrb.event.WDNetworkEvent;

import java.util.*;

/**
 * Implementierung von Playwrights Request-Schnittstelle.
 * Die Klasse verarbeitet Netzwerkereignisse aus dem WDNetworkEvent ohne Reflektion.
 */
public class RequestImpl implements Request {

    private final String url;
    private final String method;
    private final Map<String, String> allHeaders;
    private final List<HttpHeader> headersArray;
    private final String resourceType;
    private final boolean navigationRequest;
    private final String failure;
    private final Response response;

    /** Konstruktor für network.beforeRequestSent */
    public RequestImpl(WDNetworkEvent.BeforeRequestSent beforeRequestSent) {
        WDNetworkEvent.BeforeRequestSent.BeforeRequestSentParametersWD params =
                beforeRequestSent != null ? beforeRequestSent.getParams() : null;
        de.bund.zrb.type.network.WDRequestData reqData = params != null ? params.getRequest() : null;

        this.url = reqData != null && reqData.getUrl() != null ? reqData.getUrl() : "";
        this.method = reqData != null && reqData.getMethod() != null ? reqData.getMethod() : "";

        List<de.bund.zrb.type.network.WDHeader> hdrs =
                reqData != null ? reqData.getHeaders() : null;
        this.allHeaders = Collections.unmodifiableMap(convertHeadersToMap(hdrs));
        this.headersArray = Collections.unmodifiableList(convertHeadersToList(hdrs));

        String dest = reqData != null ? reqData.getDestination() : null;
        this.resourceType = inferResourceType(dest, allHeaders);

        this.navigationRequest = params != null && params.getNavigation() != null;
        this.failure = "";
        this.response = null;
    }

    /** Konstruktor für network.authRequired */
    public RequestImpl(WDNetworkEvent.AuthRequired authRequired) {
        WDNetworkEvent.AuthRequired.AuthRequiredParametersWD params =
                authRequired != null ? authRequired.getParams() : null;
        de.bund.zrb.type.network.WDRequestData reqData = params != null ? params.getRequest() : null;

        this.url = reqData != null && reqData.getUrl() != null ? reqData.getUrl() : "";
        this.method = reqData != null && reqData.getMethod() != null ? reqData.getMethod() : "";

        List<de.bund.zrb.type.network.WDHeader> hdrs =
                reqData != null ? reqData.getHeaders() : null;
        this.allHeaders = Collections.unmodifiableMap(convertHeadersToMap(hdrs));
        this.headersArray = Collections.unmodifiableList(convertHeadersToList(hdrs));

        String dest = reqData != null ? reqData.getDestination() : null;
        this.resourceType = inferResourceType(dest, allHeaders);

        this.navigationRequest = params != null && params.getNavigation() != null;
        this.failure = "";
        this.response = null;
    }

    /** Konstruktor für network.responseCompleted (entspricht onRequestFinished) */
    public RequestImpl(WDNetworkEvent.ResponseCompleted completed) {
        WDNetworkEvent.ResponseCompleted.ResponseCompletedParametersWD params =
                completed != null ? completed.getParams() : null;
        de.bund.zrb.type.network.WDRequestData reqData = params != null ? params.getRequest() : null;
        de.bund.zrb.type.network.WDResponseData respData = params != null ? params.getResponse() : null;

        this.url = reqData != null && reqData.getUrl() != null ? reqData.getUrl() : "";
        this.method = reqData != null && reqData.getMethod() != null ? reqData.getMethod() : "";

        List<de.bund.zrb.type.network.WDHeader> hdrs =
                reqData != null ? reqData.getHeaders() : null;
        this.allHeaders = Collections.unmodifiableMap(convertHeadersToMap(hdrs));
        this.headersArray = Collections.unmodifiableList(convertHeadersToList(hdrs));

        String dest = reqData != null ? reqData.getDestination() : null;
        this.resourceType = inferResourceType(dest, allHeaders);

        this.navigationRequest = params != null && params.getNavigation() != null;
        this.failure = "";

        // Response zusammenbauen
        this.response = respData != null ? new MinimalResponse(respData, this) : null;
    }

    /** Konstruktor für network.fetchError (entspricht onRequestFailed) */
    public RequestImpl(WDNetworkEvent.FetchError fetchError) {
        WDNetworkEvent.FetchError.FetchErrorParametersWD params =
                fetchError != null ? fetchError.getParams() : null;
        de.bund.zrb.type.network.WDRequestData reqData = params != null ? params.getRequest() : null;

        this.url = reqData != null && reqData.getUrl() != null ? reqData.getUrl() : "";
        this.method = reqData != null && reqData.getMethod() != null ? reqData.getMethod() : "";

        List<de.bund.zrb.type.network.WDHeader> hdrs =
                reqData != null ? reqData.getHeaders() : null;
        this.allHeaders = Collections.unmodifiableMap(convertHeadersToMap(hdrs));
        this.headersArray = Collections.unmodifiableList(convertHeadersToList(hdrs));

        String dest = reqData != null ? reqData.getDestination() : null;
        this.resourceType = inferResourceType(dest, allHeaders);

        this.navigationRequest = params != null && params.getNavigation() != null;
        this.failure = params != null && params.getErrorText() != null ? params.getErrorText() : "";
        this.response = null;
    }

    // ----- Request interface implementation -----
    @Override public Map<String, String> allHeaders() { return allHeaders; }
    @Override public String failure() { return failure; }
    @Override public Frame frame() { return null; }
    @Override public Map<String, String> headers() { return allHeaders; }
    @Override public List<HttpHeader> headersArray() { return headersArray; }
    @Override public String headerValue(String name) {
        if (name == null) return null;
        return allHeaders.getOrDefault(name.toLowerCase(), "");
    }
    @Override public boolean isNavigationRequest() { return navigationRequest; }
    @Override public String method() { return method; }
    @Override public String postData() { return null; }
    @Override public byte[] postDataBuffer() { return null; }
    @Override public Request redirectedFrom() { return null; }
    @Override public Request redirectedTo() { return null; }
    @Override public String resourceType() { return resourceType; }
    @Override public Response response() { return response; }
    @Override public Sizes sizes() { return null; }
    @Override public Timing timing() { return null; }
    @Override public String url() { return url; }

    // ----- Hilfsfunktionen -----
    private Map<String, String> convertHeadersToMap(List<de.bund.zrb.type.network.WDHeader> headersRaw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (headersRaw != null) {
            for (de.bund.zrb.type.network.WDHeader h : headersRaw) {
                if (h == null) continue;
                String name = h.getName();
                if (name == null) continue;
                String value = "";
                de.bund.zrb.type.network.WDBytesValue v = h.getValue();
                if (v != null && v.getValue() != null) value = v.getValue();
                map.put(name.toLowerCase(), value);
            }
        }
        return map;
    }
    private List<HttpHeader> convertHeadersToList(List<de.bund.zrb.type.network.WDHeader> headersRaw) {
        List<HttpHeader> list = new ArrayList<>();
        if (headersRaw != null) {
            for (de.bund.zrb.type.network.WDHeader h : headersRaw) {
                if (h == null) continue;
                String name = h.getName();
                if (name == null) continue;
                String value = "";
                de.bund.zrb.type.network.WDBytesValue v = h.getValue();
                if (v != null && v.getValue() != null) value = v.getValue();
                // HttpHeader hat einen (name, value)-Konstruktor
                list.add(new HttpHeader(name.toLowerCase(), value));
            }
        }
        return list;
    }
    private String inferResourceType(String dest, Map<String, String> flatHeaders) {
        if (dest != null && !dest.isEmpty()) {
            String d = dest.toLowerCase();
            switch (d) {
                case "document": return "document";
                case "style":
                case "stylesheet": return "stylesheet";
                case "image": return "image";
                case "media": return "media";
                case "font": return "font";
                case "script": return "script";
                case "xhr": return "xhr";
                case "fetch": return "fetch";
                default: break;
            }
        }
        if (flatHeaders != null) {
            String accept = flatHeaders.get("accept");
            if (accept != null) {
                String a = accept.toLowerCase();
                if (a.contains("text/html")) return "document";
                if (a.contains("text/css")) return "stylesheet";
                if (a.contains("image/")) return "image";
                if (a.contains("javascript")) return "script";
            }
            String xrw = flatHeaders.get("x-requested-with");
            if ("xmlhttprequest".equalsIgnoreCase(xrw)) return "xhr";
            String mode = flatHeaders.get("sec-fetch-mode");
            if ("cors".equalsIgnoreCase(mode) || "no-cors".equalsIgnoreCase(mode)) return "fetch";
        }
        return "fetch";
    }

    // ----- Minimale Response für ResponseCompleted -----
    private static class MinimalResponse implements Response {
        private final String url;
        private final long status;
        private final String mime;
        private final Map<String, String> headers;
        private final List<HttpHeader> headerList;
        private final Request request;

        MinimalResponse(de.bund.zrb.type.network.WDResponseData data, Request request) {
            this.url = data != null && data.getUrl() != null ? data.getUrl() : "";
            this.status = data != null ? data.getStatus() : 0L;
            this.mime = data != null && data.getMimeType() != null ? data.getMimeType() : "";
            Map<String, String> map = new LinkedHashMap<>();
            List<HttpHeader> arr = new ArrayList<>();
            List<de.bund.zrb.type.network.WDHeader> hdrs = data != null ? data.getHeaders() : null;
            if (hdrs != null) {
                for (de.bund.zrb.type.network.WDHeader h : hdrs) {
                    if (h == null) continue;
                    String name = h.getName();
                    String value = "";
                    de.bund.zrb.type.network.WDBytesValue v = h.getValue();
                    if (v != null && v.getValue() != null) value = v.getValue();
                    if (name != null) {
                        map.put(name.toLowerCase(), value);
                        arr.add(new HttpHeader(name.toLowerCase(), value));
                    }
                }
            }
            this.headers = Collections.unmodifiableMap(map);
            this.headerList = Collections.unmodifiableList(arr);
            this.request = request;
        }

        @Override public Map<String, String> allHeaders() { return headers; }
        @Override public byte[] body() { throw new PlaywrightException("Response body is not available for completed responses."); }
        @Override public String finished() { return null; }
        @Override public Frame frame() { return null; }
        @Override public boolean fromServiceWorker() { return false; }
        @Override public Map<String, String> headers() { return headers; }
        @Override public List<HttpHeader> headersArray() { return headerList; }
        @Override public String headerValue(String name) {
            return name == null ? null : headers.get(name.toLowerCase());
        }
        @Override public List<String> headerValues(String name) {
            String v = headerValue(name);
            return v == null ? Collections.emptyList() : Collections.singletonList(v);
        }
        @Override public boolean ok() { return status >= 200 && status < 300; }
        @Override public Request request() { return request; }
        @Override public com.microsoft.playwright.options.SecurityDetails securityDetails() { return null; }
        @Override public com.microsoft.playwright.options.ServerAddr serverAddr() { return null; }
        @Override public long status() { return status; }
        @Override public String statusText() { return ok() ? "OK" : ""; }
        @Override public String text() { throw new PlaywrightException("Response body is not available for completed responses."); }
        @Override public String url() { return url; }
    }
}
