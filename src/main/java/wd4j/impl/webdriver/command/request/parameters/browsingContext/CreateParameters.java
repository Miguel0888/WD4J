package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browser.UserContext;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

public class CreateParameters implements Command.Params {
    private final CreateType type;
    private final BrowsingContext referenceContext;
    private final boolean background;
    private final UserContext userContext;

    public CreateParameters(CreateType type, BrowsingContext referenceContext, boolean background, UserContext userContext) {
        this.type = type;
        this.referenceContext = referenceContext;
        this.background = background;
        this.userContext = userContext;
    }

    public CreateType getType() {
        return type;
    }

    public BrowsingContext getReferenceContext() {
        return referenceContext;
    }

    public boolean isBackground() {
        return background;
    }

    public UserContext getUserContext() {
        return userContext;
    }
}
