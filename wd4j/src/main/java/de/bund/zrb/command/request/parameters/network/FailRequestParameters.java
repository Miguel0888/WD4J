package de.bund.zrb.command.request.parameters.network;

import de.bund.zrb.type.network.WDRequest;
import de.bund.zrb.api.WDCommand;

public class FailRequestParameters implements WDCommand.Params {
    private final WDRequest request;

    public FailRequestParameters(WDRequest request) {
        this.request = request;
    }

    public WDRequest getRequest() {
        return request;
    }
}
