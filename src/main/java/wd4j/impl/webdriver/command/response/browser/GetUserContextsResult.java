package wd4j.impl.webdriver.command.response.browser;

import wd4j.impl.markerInterfaces.resultData.BrowserResult;
import wd4j.impl.webdriver.type.browser.UserContextInfo;

import java.util.List;

public class GetUserContextsResult implements BrowserResult {
    List<UserContextInfo> userContexts;

    public GetUserContextsResult(List<UserContextInfo> clientWindows) {
        this.userContexts = clientWindows;
    }

    public List<UserContextInfo> getUserContexts() {
        return userContexts;
    }
}
