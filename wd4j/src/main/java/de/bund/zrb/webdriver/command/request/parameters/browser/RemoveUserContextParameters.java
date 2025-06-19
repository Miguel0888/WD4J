package de.bund.zrb.webdriver.command.request.parameters.browser;

import de.bund.zrb.webdriver.type.browser.WDUserContext;
import de.bund.zrb.websocket.WDCommand;

public class RemoveUserContextParameters implements WDCommand.Params {
    private final WDUserContext WDUserContext;

    public RemoveUserContextParameters(WDUserContext WDUserContext) {
        this.WDUserContext = WDUserContext;
    }

    public WDUserContext getUserContext() {
        return WDUserContext;
    }
}
