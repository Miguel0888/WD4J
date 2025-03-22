package wd4j.impl.dto.command.request.parameters.network;

import wd4j.impl.dto.type.network.WDIntercept;
import wd4j.impl.websocket.WDCommand;

public class RemoveInterceptParameters implements WDCommand.Params {
    private final WDIntercept WDIntercept;

    public RemoveInterceptParameters(WDIntercept WDIntercept) {
        this.WDIntercept = WDIntercept;
    }

    public WDIntercept getIntercept() {
        return WDIntercept;
    }
}
