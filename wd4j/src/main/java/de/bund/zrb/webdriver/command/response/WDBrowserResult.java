package de.bund.zrb.webdriver.command.response;

import de.bund.zrb.markerInterfaces.WDResultData;
import de.bund.zrb.webdriver.type.browser.WDClientWindowInfo;
import de.bund.zrb.webdriver.type.browser.WDUserContext;
import de.bund.zrb.webdriver.type.browser.WDUserContextInfo;

import java.util.List;

public interface WDBrowserResult extends WDResultData {

    class CreateUserContextResult extends WDUserContextInfo implements WDBrowserResult {
        public CreateUserContextResult(WDUserContext userContext) {
            super(userContext);
        }
    }

    class GetClientWindowsResult implements WDBrowserResult {
        private List<WDClientWindowInfo> clientWindows;

        public GetClientWindowsResult(List<WDClientWindowInfo> clientWindows) {
            this.clientWindows = clientWindows;
        }

        public List<WDClientWindowInfo> getClientWindows() {
            return clientWindows;
        }
    }

    class GetUserContextsResult implements WDBrowserResult {
        private List<WDUserContextInfo> userContexts;

        public GetUserContextsResult(List<WDUserContextInfo> clientWindows) {
            this.userContexts = clientWindows;
        }

        public List<WDUserContextInfo> getUserContexts() {
            return userContexts;
        }
    }
}


