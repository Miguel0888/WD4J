package wd4j.impl.webdriver.command.response.browser;

import wd4j.impl.markerInterfaces.resultData.BrowserResult;
import wd4j.impl.webdriver.type.browser.ClientWindowInfo;

import java.util.List;

public class GetClientWindowsResult implements BrowserResult {
    List<ClientWindowInfo> clientWindows;

    public GetClientWindowsResult(List<ClientWindowInfo> clientWindows) {
        this.clientWindows = clientWindows;
    }

    public List<ClientWindowInfo> getClientWindows() {
        return clientWindows;
    }
}
