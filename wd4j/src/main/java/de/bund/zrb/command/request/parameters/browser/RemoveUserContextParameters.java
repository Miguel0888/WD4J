package de.bund.zrb.command.request.parameters.browser;

import de.bund.zrb.type.browser.WDUserContext;
import de.bund.zrb.api.WDCommand;

public class RemoveUserContextParameters implements WDCommand.Params {
    private final WDUserContext WDUserContext;

    public RemoveUserContextParameters(WDUserContext WDUserContext) {
        this.WDUserContext = WDUserContext;
    }

    public WDUserContext getUserContext() {
        return WDUserContext;
    }
}
