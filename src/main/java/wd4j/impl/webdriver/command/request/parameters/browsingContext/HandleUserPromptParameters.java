package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.websocket.WDCommand;

/**
 * The browsingContext.handleUserPrompt command allows closing an open prompt
 * dialog, accepting or dismissing it, and setting its input text.
 */
public class HandleUserPromptParameters implements WDCommand.Params {
    private final WDBrowsingContext context;
    private final Boolean accept; // optional
    private final String userText; // optional

    public HandleUserPromptParameters(WDBrowsingContext context) {
        this(context, null, null);
    }

    public HandleUserPromptParameters(WDBrowsingContext context, Boolean accept) {
        this(context, accept, null);
    }

    public HandleUserPromptParameters(WDBrowsingContext context, Boolean accept, String userText) {
        this.context = context;
        this.accept = accept;
        this.userText = userText;
    }

    public WDBrowsingContext getContext() {
        return context;
    }

    public Boolean getAccept() {
        return accept;
    }

    public String getUserText() {
        return userText;
    }
}
