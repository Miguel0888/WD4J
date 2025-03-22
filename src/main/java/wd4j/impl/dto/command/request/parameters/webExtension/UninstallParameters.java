package wd4j.impl.dto.command.request.parameters.webExtension;

import wd4j.impl.dto.type.webExtension.WDExtension;
import wd4j.impl.websocket.WDCommand;

public class UninstallParameters implements WDCommand.Params {
    private final WDExtension WDExtension;

    public UninstallParameters(WDExtension WDExtension) {
        this.WDExtension = WDExtension;
    }

    public WDExtension getExtension() {
        return WDExtension;
    }
}
