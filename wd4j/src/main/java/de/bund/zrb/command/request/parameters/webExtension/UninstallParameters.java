package de.bund.zrb.command.request.parameters.webExtension;

import de.bund.zrb.type.webExtension.WDExtension;
import de.bund.zrb.api.WDCommand;

public class UninstallParameters implements WDCommand.Params {
    private final WDExtension WDExtension;

    public UninstallParameters(WDExtension WDExtension) {
        this.WDExtension = WDExtension;
    }

    public WDExtension getExtension() {
        return WDExtension;
    }
}
