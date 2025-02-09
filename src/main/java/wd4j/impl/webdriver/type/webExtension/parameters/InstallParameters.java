package wd4j.impl.webdriver.type.webExtension.parameters;

import wd4j.impl.websocket.Command;

public class InstallParameters implements Command.Params {
    private final ExtensionData extensionData;

    public InstallParameters(ExtensionData extensionData) {
        this.extensionData = extensionData;
    }

    public ExtensionData getExtensionData() {
        return extensionData;
    }
}
