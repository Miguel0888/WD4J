package wd4j.impl.webdriver.type.network;

public class Request {
    private final String url;
    private final String method;

    public Request(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }
}