package de.bund.zrb.event;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.HttpHeader;
import com.microsoft.playwright.options.SecurityDetails;
import com.microsoft.playwright.options.ServerAddr;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.type.network.WDBaseParameters;
import de.bund.zrb.type.network.WDResponseData;
import de.bund.zrb.type.network.WDHeader;

import java.util.*;
import java.util.stream.Collectors;

public class ResponseImpl implements Response {

    private final WDBaseParameters rawParams;
    private final WDResponseData responseData;
    private final String errorText;
    private final Request request;
    private final Frame frame;
    private final byte[] responseBody;

    public ResponseImpl(WDNetworkEvent.ResponseStarted event, byte[] responseBody) {
        this.rawParams = event.getParams(); // 🔹 Speichere das komplette Event-Objekt
        this.responseData = event.getParams().getResponse();
        this.request = null; // TODO: Mapping von `request`
        this.frame = null; // TODO: Mapping von `frame`
        this.responseBody = responseBody; // 🔹 Response-Body speichern

        errorText = null;
    }

    public ResponseImpl(WDNetworkEvent.ResponseCompleted event, byte[] responseBody) {
        this.rawParams = event.getParams(); // 🔹 Speichert das gesamte Event-DTO
        this.responseData = event.getParams().getResponse();
        this.request = null; // TODO: Mapping von `request`
        this.frame = null; // TODO: Mapping von Frame falls möglich
        this.responseBody = responseBody;

        errorText = null;
    }

    public ResponseImpl(WDNetworkEvent.FetchError event, byte[] responseBody) {
        this.rawParams = event.getParams(); // 🔹 Speichere das komplette Event-Objekt
        this.errorText = event.getParams().getErrorText();
        this.request = null; // TODO: Mapping von `request`
        this.frame = null; // TODO: Mapping von Frame falls möglich
        this.responseBody = responseBody;

        responseData = null;
    }

    private List<HttpHeader> convertHeaders(List<WDHeader> headers) {
        if (headers == null) return Collections.emptyList();
        return headers.stream()
                .map(header -> new HttpHeader(header.getName(), extractHeaderValue(header)))
                .collect(Collectors.toList());
    }

    private Map<String, String> convertHeadersToMap(List<WDHeader> headers) {
        if (headers == null) return Collections.emptyMap();
        return headers.stream().collect(Collectors.toMap(WDHeader::getName, this::extractHeaderValue, (a, b) -> b));
    }

    private String extractHeaderValue(WDHeader header) {
        return header.getValue() != null ? header.getValue().getValue() : "";
    }

    @Override
    public Map<String, String> allHeaders() {
        return responseData != null ? convertHeadersToMap(responseData.getHeaders()) : Collections.emptyMap();
    }

    @Override
    public byte[] body() {
        if (responseBody == null) {
            throw new IllegalStateException("Response body is not available.");
        }
        return responseBody;
    }

    @Override
    public String finished() {
        return responseData != null ? responseData.getProtocol() : "unknown";
    }

    @Override
    public Frame frame() {
        return frame;
    }

    @Override
    public boolean fromServiceWorker() {
        return responseData != null && responseData.getFromCache();
    }

    @Override
    public Map<String, String> headers() {
        return allHeaders();
    }

    @Override
    public List<HttpHeader> headersArray() {
        return responseData != null ? convertHeaders(responseData.getHeaders()) : Collections.emptyList();
    }

    @Override
    public String headerValue(String name) {
        return headers().getOrDefault(name, "");
    }

    @Override
    public List<String> headerValues(String name) {
        return Collections.singletonList(headerValue(name));
    }

    @Override
    public boolean ok() {
        return responseData != null && responseData.getStatus() >= 200 && responseData.getStatus() < 300;
    }

    @Override
    public Request request() {
        return request;
    }

    @Override
    public SecurityDetails securityDetails() {
        return null; // TODO: SecurityDetails-Mapping hinzufügen
    }

    @Override
    public ServerAddr serverAddr() {
        return null; // TODO: ServerAddr-Mapping hinzufügen
    }

    @Override
    public long status() {
        return responseData != null ? responseData.getStatus() : -1;
    }

    @Override
    public String statusText() {
        return responseData != null ? responseData.getStatusText() : "unknown";
    }

    @Override
    public String text() {
        if (responseBody == null) {
            throw new IllegalStateException("Response body is not available.");
        }
        return new String(responseBody);
    }

    @Override
    public String url() {
        return responseData != null ? responseData.getUrl() : "unknown";
    }

    public WDBaseParameters getRawParams() {
        return rawParams;
    }
}
