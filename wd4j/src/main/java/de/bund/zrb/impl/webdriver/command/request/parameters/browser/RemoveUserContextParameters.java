package de.bund.zrb.impl.webdriver.command.request.parameters.browser;

import de.bund.zrb.impl.webdriver.type.browser.WDUserContext;
import de.bund.zrb.impl.websocket.WDCommand;

public class RemoveUserContextParameters implements WDCommand.Params {
    private final WDUserContext WDUserContext;

    public RemoveUserContextParameters(WDUserContext WDUserContext) {
        this.WDUserContext = WDUserContext;
    }

    public WDUserContext getUserContext() {
        return WDUserContext;
    }
}
