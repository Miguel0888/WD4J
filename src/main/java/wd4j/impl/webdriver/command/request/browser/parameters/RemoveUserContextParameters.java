package wd4j.impl.webdriver.command.request.browser.parameters;

import wd4j.api.BrowserContext;
import wd4j.impl.webdriver.type.browser.UserContext;
import wd4j.impl.websocket.Command;

public class RemoveUserContextParameters implements Command.Params {
    private final UserContext userContext;

    public RemoveUserContextParameters(UserContext userContext) {
        this.userContext = userContext;
    }

    public UserContext getUserContext() {
        return userContext;
    }
}
