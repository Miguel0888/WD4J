package wd4j.impl.webdriver.command.response;

import wd4j.impl.webdriver.type.browser.WDClientWindowInfo;
import wd4j.impl.webdriver.type.browser.WDUserContext;
import wd4j.impl.webdriver.type.browser.WDUserContextInfo;

import java.util.List;

public interface WDBrowserResult {

    class CreateWDUserContextWDBrowserResult extends WDUserContextInfo implements WDBrowserResult {
        public CreateWDUserContextWDBrowserResult(WDUserContext WDUserContextId) {
            super(WDUserContextId);
        }
    }

    class GetUserContextsWDBrowserResult implements WDBrowserResult {
        List<WDUserContextInfo> userContexts;

        public GetUserContextsWDBrowserResult(List<WDUserContextInfo> clientWindows) {
            this.userContexts = clientWindows;
        }

        public List<WDUserContextInfo> getUserContexts() {
            return userContexts;
        }
    }

    class GetClientWindowsWDBrowserResult implements WDBrowserResult {
        List<WDClientWindowInfo> clientWindows;

        public GetClientWindowsWDBrowserResult(List<WDClientWindowInfo> clientWindows) {
            this.clientWindows = clientWindows;
        }

        public List<WDClientWindowInfo> getClientWindows() {
            return clientWindows;
        }
    }
}


