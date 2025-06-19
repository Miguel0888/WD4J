package de.bund.zrb.command.response;

import de.bund.zrb.api.markerInterfaces.WDResultData;
import de.bund.zrb.type.webExtension.WDExtension;

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
