package wd4j.impl.webdriver.type.network;

public class ResponseData {
    private final int status;
    private final String statusText;

    public ResponseData(int status, String statusText) {
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