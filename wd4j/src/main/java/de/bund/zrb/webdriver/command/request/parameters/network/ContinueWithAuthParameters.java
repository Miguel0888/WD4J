package de.bund.zrb.webdriver.command.request.parameters.network;

import de.bund.zrb.webdriver.mapping.EnumWrapper;
import de.bund.zrb.webdriver.type.network.WDRequest;
import de.bund.zrb.websocket.WDCommand;

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
