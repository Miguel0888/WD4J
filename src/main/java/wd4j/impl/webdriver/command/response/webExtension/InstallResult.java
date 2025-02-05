package wd4j.impl.webdriver.command.response.webExtension;

import wd4j.impl.markerInterfaces.resultData.WebExtensionResult;
import wd4j.impl.webdriver.type.webExtension.Extension;

public class InstallResult implements WebExtensionResult {
    private final Extension extension;

    public InstallResult(Extension extension) {
        this.extension = extension;
    }

    public Extension getExtension() {
        return extension;
    }
}
