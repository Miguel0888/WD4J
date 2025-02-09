package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

public class CloseParameters implements Command.Params {
    private final BrowsingContext context;
    private final Boolean promptUnload; // Optional

    public CloseParameters(BrowsingContext context, boolean promptUnload) {
        this.context = context;
        this.promptUnload = promptUnload;
    }

    public CloseParameters(BrowsingContext context) {
        this.context = context;
        this.promptUnload = null;
    }

    public BrowsingContext getContext() {
        return context;
    }

    public Boolean getPromptUnload() {
        return promptUnload;
    }

}
