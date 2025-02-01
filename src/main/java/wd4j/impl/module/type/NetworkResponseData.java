package wd4j.impl.module.type;

public class NetworkResponseData {
    private final int status;
    private final String statusText;

    public NetworkResponseData(int status, String statusText) {
        this.status = status;
        this.statusText = statusText;
    }

    public int getStatus() {
        return status;
    }

    public String getStatusText() {
        return statusText;
    }
}