package wd4j.impl.webdriver.type.webExtension.parameters;

import wd4j.impl.webdriver.type.webExtension.Extension;
import wd4j.impl.webdriver.type.webExtension.ExtensionData;
import wd4j.impl.websocket.Command;

public class UninstallParameters implements Command.Params {
    private final Extension extension;

    public UninstallParameters(Extension extension) {
        this.extension = extension;
    }

    public Extension getExtension() {
        return extension;
    }
}
