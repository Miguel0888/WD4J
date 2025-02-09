package wd4j.impl.webdriver.type.network;

import java.util.List;

public class ResponseData {
    private final String url;
    private final String protocol;
    private final char status;
    private final String statusText;
    private final boolean fromCache;
    private final List<Header> headers;
    private final String mimeType;
    private final char bytesReceived;
    private final char headersSize;
    private final char bodySize;
    private final  ResponseContent content;
    private final List<AuthChallenge> authChallenges; // optional

    public ResponseData(String url, String protocol, char status, String statusText, boolean fromCache, List<Header> headers, String mimeType, char bytesReceived, char headersSize, char bodySize, ResponseContent content) {
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

    public ResponseData(String url, String protocol, char status, String statusText, boolean fromCache, List<Header> headers, String mimeType, char bytesReceived, char headersSize, char bodySize, ResponseContent content, List<AuthChallenge> authChallenges) {
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

    public List<Header> getHeaders() {
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

    public ResponseContent getContent() {
        return content;
    }

    public List<AuthChallenge> getAuthChallenges() {
        return authChallenges;
    }
}
