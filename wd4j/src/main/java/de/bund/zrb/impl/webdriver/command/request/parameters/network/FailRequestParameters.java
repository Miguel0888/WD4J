package de.bund.zrb.impl.webdriver.command.request.parameters.network;

import de.bund.zrb.impl.webdriver.type.network.WDRequest;
import de.bund.zrb.impl.websocket.WDCommand;

public class FailRequestParameters implements WDCommand.Params {
    private final WDRequest WDRequest;

    public FailRequestParameters(WDRequest WDRequest) {
        this.WDRequest = WDRequest;
    }

    public WDRequest getRequest() {
        return WDRequest;
    }
}
