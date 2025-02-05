package wd4j.impl.webdriver.type.browsingContext.parameters;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

public class CloseParameters implements Command.Params {
    private final BrowsingContext context;
    private final boolean promptUnload;

    public CloseParameters(BrowsingContext context, boolean promptUnload) {
        this.context = context;
        this.promptUnload = promptUnload;
    }

    public BrowsingContext getContext() {
        return context;
    }

    public boolean getPromptUnload() {
        return promptUnload;
    }

}
