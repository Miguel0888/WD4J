package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.ResultData;
import wd4j.impl.webdriver.type.webExtension.Extension;

public interface WebExtensionResult extends ResultData {
    class InstallResult implements WebExtensionResult {
        private final Extension extension;

        public InstallResult(Extension extension) {
            this.extension = extension;
        }

        public Extension getExtension() {
            return extension;
        }
    }
}
