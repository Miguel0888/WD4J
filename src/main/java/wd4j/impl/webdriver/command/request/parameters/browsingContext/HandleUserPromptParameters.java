package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

/**
 * The browsingContext.handleUserPrompt command allows closing an open prompt
 * dialog, accepting or dismissing it, and setting its input text.
 */
public class HandleUserPromptParameters implements Command.Params {
    private final BrowsingContext context;
    private final Boolean accept; // optional
    private final String userText; // optional

    public HandleUserPromptParameters(BrowsingContext context) {
        this(context, null, null);
    }

    public HandleUserPromptParameters(BrowsingContext context, Boolean accept) {
        this(context, accept, null);
    }

    public HandleUserPromptParameters(BrowsingContext context, Boolean accept, String userText) {
        this.context = context;
        this.accept = accept;
        this.userText = userText;
    }

    public BrowsingContext getContext() {
        return context;
    }

    public Boolean getAccept() {
        return accept;
    }

    public String getUserText() {
        return userText;
    }
}
