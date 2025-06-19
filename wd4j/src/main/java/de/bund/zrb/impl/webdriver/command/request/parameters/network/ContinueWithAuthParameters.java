package de.bund.zrb.impl.webdriver.command.request.parameters.network;

import de.bund.zrb.impl.webdriver.mapping.EnumWrapper;
import de.bund.zrb.impl.webdriver.type.network.WDRequest;
import de.bund.zrb.impl.websocket.WDCommand;

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
