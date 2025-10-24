package de.bund.zrb.command.request.parameters.network;

import de.bund.zrb.type.network.WDIntercept;
import de.bund.zrb.api.WDCommand;

public class RemoveInterceptParameters implements WDCommand.Params {
    private final WDIntercept intercept;

    public RemoveInterceptParameters(WDIntercept intercept) {
        this.intercept = intercept;
    }

    public WDIntercept getIntercept() {
        return intercept;
    }
}
