package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.WDResultData;
import wd4j.impl.webdriver.type.webExtension.WDExtension;

public interface WDWebExtensionResult extends WDResultData {
    class InstallWDWebExtensionResult implements WDWebExtensionResult {
        private final WDExtension WDExtension;

        public InstallWDWebExtensionResult(WDExtension WDExtension) {
            this.WDExtension = WDExtension;
        }

        public WDExtension getExtension() {
            return WDExtension;
        }
    }
}
