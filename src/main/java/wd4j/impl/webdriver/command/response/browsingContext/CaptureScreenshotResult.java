package wd4j.impl.webdriver.command.response.browsingContext;

import wd4j.impl.markerInterfaces.resultData.BrowsingContextResult;

public class CaptureScreenshotResult implements BrowsingContextResult {
    private String data;

    public CaptureScreenshotResult(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "CaptureScreenshotResult{" +
                "data='" + data + '\'' +
                '}';
    }
}
