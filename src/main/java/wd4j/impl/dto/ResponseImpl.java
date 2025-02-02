package wd4j.impl.dto;

import wd4j.api.Response;
import wd4j.api.Request;
import wd4j.api.Frame;
import wd4j.api.options.HttpHeader;
import wd4j.api.options.SecurityDetails;
import wd4j.api.options.ServerAddr;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ResponseImpl implements Response {
    private final String url;
    private final int status;
    private final String statusText;
    private final Map<String, String> headers;
    private final byte[] body;
    private final Request request;
    private final Frame frame;
    private final SecurityDetails securityDetails;
    private final ServerAddr serverAddr;
    private final boolean fromServiceWorker;
    private final boolean ok;
    private final List<HttpHeader> headersArray;

    public ResponseImpl(
            String url, int status, String statusText, Map<String, String> headers, byte[] body, Request request,
            Frame frame, SecurityDetails securityDetails, ServerAddr serverAddr, boolean fromServiceWorker, boolean ok,
            List<HttpHeader> headersArray) {
        this.url = url;
        this.status = status;
        this.statusText = statusText;
        this.headers = headers;
        this.body = body;
        this.request = request;
        this.frame = frame;
        this.securityDetails = securityDetails;
        this.serverAddr = serverAddr;
        this.fromServiceWorker = fromServiceWorker;
        this.ok = ok;
        this.headersArray = headersArray;
    }

    @Override
    public Map<String, String> allHeaders() {
        return headers;
    }

    @Override
    public byte[] body() {
        return body;
    }

    @Override
    public String finished() {
        return null;
    }

    @Override
    public Frame frame() {
        return frame;
    }

    @Override
    public boolean fromServiceWorker() {
        return fromServiceWorker;
    }

    @Override
    public Map<String, String> headers() {
        return headers;
    }

    @Override
    public List<HttpHeader> headersArray() {
        return headersArray;
    }

    @Override
    public String headerValue(String name) {
        return headers.getOrDefault(name, "");
    }

    @Override
    public List<String> headerValues(String name) {
        return Collections.singletonList(headers.getOrDefault(name, ""));
    }

    @Override
    public boolean ok() {
        return ok;
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public String statusText() {
        return statusText;
    }

    @Override
    public String text() {
        return new String(body);
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public Request request() {
        return request;
    }

    @Override
    public SecurityDetails securityDetails() {
        return securityDetails;
    }

    @Override
    public ServerAddr serverAddr() {
        return serverAddr;
    }
}
