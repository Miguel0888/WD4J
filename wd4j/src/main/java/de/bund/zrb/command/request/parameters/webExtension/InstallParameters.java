package de.bund.zrb.command.request.parameters.webExtension;

import de.bund.zrb.api.WDCommand;

public class InstallParameters implements WDCommand.Params {
    private final ExtensionData extensionData;

    public InstallParameters(ExtensionData extensionData) {
        this.extensionData = extensionData;
    }

    public ExtensionData getExtensionData() {
        return extensionData;
    }
}
