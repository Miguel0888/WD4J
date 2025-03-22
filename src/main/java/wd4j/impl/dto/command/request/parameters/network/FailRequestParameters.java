package wd4j.impl.dto.command.request.parameters.network;

import wd4j.impl.dto.type.network.WDRequest;
import wd4j.impl.websocket.WDCommand;

public class FailRequestParameters implements WDCommand.Params {
    private final WDRequest WDRequest;

    public FailRequestParameters(WDRequest WDRequest) {
        this.WDRequest = WDRequest;
    }

    public WDRequest getRequest() {
        return WDRequest;
    }
}
