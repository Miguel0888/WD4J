package de.bund.zrb.impl.webdriver.command.request.parameters.network;

import de.bund.zrb.impl.webdriver.type.network.WDIntercept;
import de.bund.zrb.impl.websocket.WDCommand;

public class RemoveInterceptParameters implements WDCommand.Params {
    private final WDIntercept WDIntercept;

    public RemoveInterceptParameters(WDIntercept WDIntercept) {
        this.WDIntercept = WDIntercept;
    }

    public WDIntercept getIntercept() {
        return WDIntercept;
    }
}
