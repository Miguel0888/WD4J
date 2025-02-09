package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browser.UserContext;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

public class CreateParameters implements Command.Params {
    private final CreateType type;
    private final BrowsingContext referenceContext; // optional
    private final Boolean background; // optional
    private final UserContext userContext; // optional

    public CreateParameters(CreateType type) {
        this(type, null, null, null);
    }

    public CreateParameters(CreateType type, BrowsingContext referenceContext, Boolean background, UserContext userContext) {
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

    public Boolean isBackground() {
        return background;
    }

    public UserContext getUserContext() {
        return userContext;
    }
}
