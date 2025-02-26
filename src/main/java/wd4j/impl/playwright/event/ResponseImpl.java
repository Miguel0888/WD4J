package wd4j.impl.playwright.event;

import wd4j.api.Frame;
import wd4j.api.Request;
import wd4j.api.Response;
import wd4j.api.options.HttpHeader;
import wd4j.api.options.SecurityDetails;
import wd4j.api.options.ServerAddr;
import wd4j.impl.webdriver.event.WDNetworkEvent;
import wd4j.impl.webdriver.event.WDNetworkEvent.ResponseStarted;
import wd4j.impl.webdriver.type.network.WDResponseData;
import wd4j.impl.webdriver.type.network.WDHeader;

import java.util.*;
import java.util.stream.Collectors;

public class ResponseImpl implements Response {

    private ResponseStarted.ResponseStartedParametersWD responseParams; // 🔹 Speichert das gesamte Event-DTO
    private WDResponseData responseData;
    private Request request;
    private Frame frame;
    private byte[] responseBody; // 🔹 Speichert den Response-Body

    public ResponseImpl(ResponseStarted event, byte[] responseBody) {
        this.responseParams = event.getParams(); // 🔹 Speichere das komplette Event-Objekt
        this.responseData = responseParams.getResponse();
        this.request = null; // TODO: Mapping von `request`
        this.frame = null; // TODO: Mapping von `frame`
        this.responseBody = responseBody; // 🔹 Response-Body speichern
    }

    public ResponseImpl(WDNetworkEvent.FetchError fetchError, Object responseBody) {
        // TODO: Mapping von `fetchError`
    }

    public ResponseImpl(WDNetworkEvent.ResponseCompleted responseCompleted, Object responseBody) {
        // TODO: Mapping von `responseCompleted`
    }

    /**
     * 🔹 Konvertiert `List<WDHeader>` zu `List<HttpHeader>` (Playwright-Format).
     */
    private List<HttpHeader> convertHeaders(List<WDHeader> headers) {
        if (headers == null) return Collections.emptyList();
        return headers.stream().map(header -> {
            HttpHeader httpHeader = new HttpHeader();
            httpHeader.name = header.getName();
            httpHeader.value = extractHeaderValue(header); // 🔹 Wert korrekt extrahieren
            return httpHeader;
        }).collect(Collectors.toList());
    }

    /**
     * 🔹 Konvertiert `List<WDHeader>` zu `Map<String, String>`, falls nötig.
     */
    private Map<String, String> convertHeadersToMap(List<WDHeader> headers) {
        if (headers == null) return Collections.emptyMap();
        return headers.stream().collect(Collectors.toMap(WDHeader::getName, this::extractHeaderValue));
    }

    /**
     * 🔹 Extrahiert den Header-Wert aus dem JSON.
     */
    private String extractHeaderValue(WDHeader header) {
        if (header.getValue() != null) {
            return header.getValue().toString(); // 🔹 Direkt als String zurückgeben (z.B. für `content-type`)
        }
        return "";
    }

    @Override
    public Map<String, String> allHeaders() {
        return convertHeadersToMap(responseData.getHeaders());
    }

    @Override
    public byte[] body() {
        return responseBody; // 🔹 Rückgabe des tatsächlichen Response-Bodys
    }

    @Override
    public String finished() {
        return responseData.getProtocol();
    }

    @Override
    public Frame frame() {
        return frame;
    }

    @Override
    public boolean fromServiceWorker() {
        return responseData.getFromCache();
    }

    @Override
    public Map<String, String> headers() {
        return convertHeadersToMap(responseData.getHeaders());
    }

    @Override
    public List<HttpHeader> headersArray() {
        return convertHeaders(responseData.getHeaders());
    }

    @Override
    public String headerValue(String name) {
        return convertHeadersToMap(responseData.getHeaders()).getOrDefault(name, "");
    }

    @Override
    public List<String> headerValues(String name) {
        return Collections.singletonList(convertHeadersToMap(responseData.getHeaders()).getOrDefault(name, ""));
    }

    @Override
    public boolean ok() {
        int status = responseData.getStatus();
        return status >= 200 && status < 300;
    }

    @Override
    public Request request() {
        return request;
    }

    @Override
    public SecurityDetails securityDetails() {
        return null; // TODO: Mapping für SecurityDetails hinzufügen
    }

    @Override
    public ServerAddr serverAddr() {
        return null; // TODO: Mapping für ServerAddr hinzufügen
    }

    @Override
    public int status() {
        return responseData.getStatus();
    }

    @Override
    public String statusText() {
        return responseData.getStatusText();
    }

    @Override
    public String text() {
        return new String(responseBody); // 🔹 Konvertiere Body in String
    }

    @Override
    public String url() {
        return responseData.getUrl();
    }

    /**
     * 🔹 Ermöglicht Zugriff auf das vollständige `ResponseStartedParametersWD`-DTO.
     */
    public ResponseStarted.ResponseStartedParametersWD getResponseParams() {
        return responseParams;
    }
}
