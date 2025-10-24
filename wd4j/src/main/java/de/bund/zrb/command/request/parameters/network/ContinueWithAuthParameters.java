package de.bund.zrb.command.request.parameters.network;

import de.bund.zrb.support.mapping.EnumWrapper;
import de.bund.zrb.type.network.WDRequest;
import de.bund.zrb.api.WDCommand;

public abstract class ContinueWithAuthParameters implements WDCommand.Params {
    private final WDRequest request;

    public ContinueWithAuthParameters(WDRequest request) {
        this.request = request;
    }

    public WDRequest getRequest() {
        return request;
    }

    public interface Action extends EnumWrapper {

    }
}
