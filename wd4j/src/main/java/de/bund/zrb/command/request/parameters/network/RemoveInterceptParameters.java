package de.bund.zrb.command.request.parameters.network;

import de.bund.zrb.type.network.WDIntercept;
import de.bund.zrb.api.WDCommand;

public class RemoveInterceptParameters implements WDCommand.Params {
    private final WDIntercept WDIntercept;

    public RemoveInterceptParameters(WDIntercept WDIntercept) {
        this.WDIntercept = WDIntercept;
    }

    public WDIntercept getIntercept() {
        return WDIntercept;
    }
}
