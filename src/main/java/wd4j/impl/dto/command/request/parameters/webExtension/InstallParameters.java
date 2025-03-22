package wd4j.impl.dto.command.request.parameters.webExtension;

import wd4j.impl.websocket.WDCommand;

public class InstallParameters implements WDCommand.Params {
    private final ExtensionData extensionData;

    public InstallParameters(ExtensionData extensionData) {
        this.extensionData = extensionData;
    }

    public ExtensionData getExtensionData() {
        return extensionData;
    }
}
