package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.WDResultData;
import wd4j.impl.webdriver.type.webExtension.WDExtension;

public interface WDWebExtensionResult extends WDResultData {
    class InstallResult implements WDWebExtensionResult {
        private final WDExtension extension;

        public InstallResult(WDExtension extension) {
            this.extension = extension;
        }

        public WDExtension getExtension() {
            return extension;
        }
    }
}
