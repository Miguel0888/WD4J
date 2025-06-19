package de.bund.zrb.webdriver.type.network;

import java.util.List;

public class WDResponseData {
    private final String url;
    private final String protocol;
    private final long status;
    private final String statusText;
    private final boolean fromCache;
    private final List<WDHeader> headers;
    private final String mimeType;
    private final long bytesReceived;
    private final long headersSize;
    private final long bodySize;
    private final WDResponseContent content;
    private final List<WDAuthChallenge> authChallenges; // optional

    public WDResponseData(String url, String protocol, int status, String statusText, boolean fromCache, List<WDHeader> headers, String mimeType, long bytesReceived, long headersSize, long bodySize, WDResponseContent content) {
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

    public WDResponseData(String url, String protocol, long status, String statusText, boolean fromCache, List<WDHeader> headers, String mimeType, long bytesReceived, long headersSize, long bodySize, WDResponseContent content, List<WDAuthChallenge> authChallenges) {
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

    public long getStatus() {
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

    public long getBytesReceived() {
        return bytesReceived;
    }

    public long getHeadersSize() {
        return headersSize;
    }

    public long getBodySize() {
        return bodySize;
    }

    public WDResponseContent getContent() {
        return content;
    }

    public List<WDAuthChallenge> getAuthChallenges() {
        return authChallenges;
    }
}
