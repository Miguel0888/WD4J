package de.bund.zrb.command.request.parameters.network;

import de.bund.zrb.type.network.WDRequest;
import de.bund.zrb.api.WDCommand;

public class FailRequestParameters implements WDCommand.Params {
    private final WDRequest WDRequest;

    public FailRequestParameters(WDRequest WDRequest) {
        this.WDRequest = WDRequest;
    }

    public WDRequest getRequest() {
        return WDRequest;
    }
}
