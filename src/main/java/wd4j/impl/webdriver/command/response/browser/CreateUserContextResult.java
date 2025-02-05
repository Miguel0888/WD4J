package wd4j.impl.webdriver.command.response.browser;

import wd4j.impl.markerInterfaces.resultData.BrowserResult;
import wd4j.impl.webdriver.type.browser.UserContextInfo;

public class CreateUserContextResult extends UserContextInfo implements BrowserResult {

    public CreateUserContextResult(String id, String type) {
        super(id, type);
    }
}
