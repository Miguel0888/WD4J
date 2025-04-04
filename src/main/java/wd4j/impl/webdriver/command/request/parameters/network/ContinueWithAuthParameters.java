package wd4j.impl.webdriver.command.request.parameters.network;

import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.type.network.WDRequest;
import wd4j.impl.websocket.WDCommand;

public abstract class ContinueWithAuthParameters implements WDCommand.Params {
    private final WDRequest WDRequest;

    public ContinueWithAuthParameters(WDRequest WDRequest) {
        this.WDRequest = WDRequest;
    }

    public WDRequest getRequest() {
        return WDRequest;
    }

    public interface Action extends EnumWrapper {

    }
}
