package de.bund.zrb.impl.webdriver.command.request.parameters.webExtension;

import de.bund.zrb.impl.webdriver.type.webExtension.WDExtension;
import de.bund.zrb.impl.websocket.WDCommand;

public class UninstallParameters implements WDCommand.Params {
    private final WDExtension WDExtension;

    public UninstallParameters(WDExtension WDExtension) {
        this.WDExtension = WDExtension;
    }

    public WDExtension getExtension() {
        return WDExtension;
    }
}
