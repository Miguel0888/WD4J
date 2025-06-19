package de.bund.zrb.webdriver.command.request.parameters.webExtension;

import de.bund.zrb.websocket.WDCommand;

public class InstallParameters implements WDCommand.Params {
    private final ExtensionData extensionData;

    public InstallParameters(ExtensionData extensionData) {
        this.extensionData = extensionData;
    }

    public ExtensionData getExtensionData() {
        return extensionData;
    }
}
