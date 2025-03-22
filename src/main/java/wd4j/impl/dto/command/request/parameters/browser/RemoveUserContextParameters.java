package wd4j.impl.dto.command.request.parameters.browser;

import wd4j.impl.dto.type.browser.WDUserContext;
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
