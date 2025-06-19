package de.bund.zrb.webdriver.command.response;

import de.bund.zrb.markerInterfaces.WDResultData;
import de.bund.zrb.webdriver.type.webExtension.WDExtension;

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
