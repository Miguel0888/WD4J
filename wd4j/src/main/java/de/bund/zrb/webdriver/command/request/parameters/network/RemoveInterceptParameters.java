package de.bund.zrb.webdriver.command.request.parameters.network;

import de.bund.zrb.webdriver.type.network.WDIntercept;
import de.bund.zrb.websocket.WDCommand;

public class RemoveInterceptParameters implements WDCommand.Params {
    private final WDIntercept WDIntercept;

    public RemoveInterceptParameters(WDIntercept WDIntercept) {
        this.WDIntercept = WDIntercept;
    }

    public WDIntercept getIntercept() {
        return WDIntercept;
    }
}
