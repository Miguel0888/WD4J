package wd4j.impl.playwright.event;

import wd4j.api.Frame;
import wd4j.api.Request;
import wd4j.api.Response;
import wd4j.api.options.HttpHeader;
import wd4j.api.options.SecurityDetails;
import wd4j.api.options.ServerAddr;
import wd4j.impl.webdriver.event.WDNetworkEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ResponseImpl implements Response {
    public ResponseImpl(WDNetworkEvent wdNetworkEvent) {
        // TODO: Implement this
    }

    @Override
    public Map<String, String> allHeaders() {
        return Collections.emptyMap();
    }

    @Override
    public byte[] body() {
        return new byte[0];
    }

    @Override
    public String finished() {
        return "";
    }

    @Override
    public Frame frame() {
        return null;
    }

    @Override
    public boolean fromServiceWorker() {
        return false;
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
    public List<String> headerValues(String name) {
        return Collections.emptyList();
    }

    @Override
    public boolean ok() {
        return false;
    }

    @Override
    public Request request() {
        return null;
    }

    @Override
    public SecurityDetails securityDetails() {
        return null;
    }

    @Override
    public ServerAddr serverAddr() {
        return null;
    }

    @Override
    public int status() {
        return 0;
    }

    @Override
    public String statusText() {
        return "";
    }

    @Override
    public String text() {
        return "";
    }

    @Override
    public String url() {
        return "";
    }
}
