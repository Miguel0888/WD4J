package wd4j.impl.webdriver.command.response.browser;

import wd4j.impl.webdriver.type.browser.ClientWindowInfo;
import wd4j.impl.webdriver.type.browser.UserContext;
import wd4j.impl.webdriver.type.browser.UserContextInfo;

import java.util.List;

public interface BrowserResult {

    class CreateUserContextResult extends UserContextInfo implements BrowserResult {
        public CreateUserContextResult(UserContext userContextId) {
            super(userContextId);
        }
    }

    class GetUserContextsResult implements BrowserResult {
        List<UserContextInfo> userContexts;

        public GetUserContextsResult(List<UserContextInfo> clientWindows) {
            this.userContexts = clientWindows;
        }

        public List<UserContextInfo> getUserContexts() {
            return userContexts;
        }
    }

    class GetClientWindowsResult implements BrowserResult {
        List<ClientWindowInfo> clientWindows;

        public GetClientWindowsResult(List<ClientWindowInfo> clientWindows) {
            this.clientWindows = clientWindows;
        }

        public List<ClientWindowInfo> getClientWindows() {
            return clientWindows;
        }
    }
}


