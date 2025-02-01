package wd4j.impl.module.type;

public class NetworkRequest {
    private final String url;
    private final String method;

    public NetworkRequest(String url, String method) {
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