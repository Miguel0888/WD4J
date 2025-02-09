package wd4j.impl.webdriver.command.request.browsingContext.parameters;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

public class HandleUserPromptParameters implements Command.Params {
    private final BrowsingContext context;
    private final boolean accept;
    private final String userText;

    public HandleUserPromptParameters(BrowsingContext context, boolean accept, String userText) {
        this.context = context;
        this.accept = accept;
        this.userText = userText;
    }

    public BrowsingContext getContext() {
        return context;
    }

    public boolean getAccept() {
        return accept;
    }

    public String getUserText() {
        return userText;
    }
}
