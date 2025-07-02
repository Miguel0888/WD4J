package de.bund.zrb.command.request.parameters.browser;

import de.bund.zrb.type.browser.WDUserContext;
import de.bund.zrb.api.WDCommand;

public class RemoveUserContextParameters implements WDCommand.Params {
    private final WDUserContext userContext;

    public RemoveUserContextParameters(WDUserContext userContext) {
        this.userContext = userContext;
    }

    public WDUserContext getUserContext() {
        return userContext;
    }
}
