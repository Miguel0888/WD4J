package wd4j.impl.webdriver.type.network;

import java.util.List;

public class WDResponseData {
    private final String url;
    private final String protocol;
    private final char status;
    private final String statusText;
    private final boolean fromCache;
    private final List<WDHeader> headers;
    private final String mimeType;
    private final char bytesReceived;
    private final char headersSize;
    private final char bodySize;
    private final WDResponseContent content;
    private final List<WDAuthChallenge> authChallenges; // optional

    public WDResponseData(String url, String protocol, char status, String statusText, boolean fromCache, List<WDHeader> headers, String mimeType, char bytesReceived, char headersSize, char bodySize, WDResponseContent content) {
        this.url = url;
        this.protocol = protocol;
        this.status = status;
        this.statusText = statusText;
        this.fromCache = fromCache;
        this.headers = headers;
        this.mimeType = mimeType;
        this.bytesReceived = bytesReceived;
        this.headersSize = headersSize;
        this.bodySize = bodySize;
        this.content = content;
        this.authChallenges = null;
    }

    public WDResponseData(String url, String protocol, char status, String statusText, boolean fromCache, List<WDHeader> headers, String mimeType, char bytesReceived, char headersSize, char bodySize, WDResponseContent content, List<WDAuthChallenge> authChallenges) {
        this.url = url;
        this.protocol = protocol;
        this.status = status;
        this.statusText = statusText;
        this.fromCache = fromCache;
        this.headers = headers;
        this.mimeType = mimeType;
        this.bytesReceived = bytesReceived;
        this.headersSize = headersSize;
        this.bodySize = bodySize;
        this.content = content;
        this.authChallenges = authChallenges;
    }

    public String getUrl() {
        return url;
    }

    public String getProtocol() {
        return protocol;
    }

    public char getStatus() {
        return status;
    }

    public String getStatusText() {
        return statusText;
    }

    public boolean getFromCache() {
        return fromCache;
    }

    public List<WDHeader> getHeaders() {
        return headers;
    }

    public String getMimeType() {
        return mimeType;
    }

    public char getBytesReceived() {
        return bytesReceived;
    }

    public char getHeadersSize() {
        return headersSize;
    }

    public char getBodySize() {
        return bodySize;
    }

    public WDResponseContent getContent() {
        return content;
    }

    public List<WDAuthChallenge> getAuthChallenges() {
        return authChallenges;
    }
}
