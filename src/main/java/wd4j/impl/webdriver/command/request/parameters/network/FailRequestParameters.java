package wd4j.impl.webdriver.command.request.parameters.network;

import wd4j.impl.webdriver.type.network.WDRequest;
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
