package wd4j.impl.webdriver.command.request.parameters.browser;

import wd4j.impl.webdriver.type.browser.WDUserContext;
import wd4j.impl.websocket.WDCommand;

public class RemoveUserContextParameters implements WDCommand.Params {
    private final WDUserContext WDUserContext;

    public RemoveUserContextParameters(WDUserContext WDUserContext) {
        this.WDUserContext = WDUserContext;
    }

    public WDUserContext getUserContext() {
        return WDUserContext;
    }
}
