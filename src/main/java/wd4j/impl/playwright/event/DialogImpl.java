package wd4j.impl.playwright.event;

import wd4j.api.Dialog;
import wd4j.api.Page;
import wd4j.impl.playwright.BrowserImpl;
import wd4j.impl.webdriver.event.WDBrowsingContextEvent;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.WDUserPromptType;
import wd4j.impl.webdriver.type.session.WDUserPromptHandlerType;

public class DialogImpl implements Dialog {
    private final String contextId;
    private final WDUserPromptType type;
    private final String message;
    private final String defaultValue;
    private boolean isAccepted;
    private String promptText;

    public DialogImpl(WDBrowsingContextEvent.UserPromptOpened userPromptOpened) {
        if (userPromptOpened == null) {
            throw new IllegalArgumentException("UserPromptOpened Event darf nicht null sein.");
        }

        WDBrowsingContextEvent.UserPromptOpened.UserPromptOpenedParameters params = userPromptOpened.getParams();
        if (params == null) {
            throw new IllegalArgumentException("UserPromptOpenedParameters dürfen nicht null sein.");
        }

        this.contextId = params.getContext();
        this.type = params.getType();
        this.message = params.getMessage();
        this.defaultValue = params.getDefaultValue();
        this.isAccepted = false;
        this.promptText = null;
    }

    public DialogImpl(WDBrowsingContextEvent.UserPromptClosed userPromptClosed) {
        if (userPromptClosed == null) {
            throw new IllegalArgumentException("UserPromptClosed Event darf nicht null sein.");
        }

        WDBrowsingContextEvent.UserPromptClosed.UserPromptClosedParameters params = userPromptClosed.getParams();
        if (params == null) {
            throw new IllegalArgumentException("UserPromptClosedParameters dürfen nicht null sein.");
        }

        this.contextId = params.getContext();
        this.type = params.getType();
        this.message = params.getUserText();
        this.defaultValue = "";
        this.isAccepted = params.isAccepted();
        this.promptText = params.getUserText();
    }

    @Override
    public void accept(String promptText) {
        this.isAccepted = true;
        this.promptText = promptText != null ? promptText : "";
        System.out.println("✅ Dialog akzeptiert: " + message + " mit Eingabe: " + this.promptText);
    }

    @Override
    public void dismiss() {
        this.isAccepted = false;
        this.promptText = null;
        System.out.println("❌ Dialog abgelehnt: " + message);
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public Page page() {
        return BrowserImpl.getPage(new WDBrowsingContext(contextId));
    }

    @Override
    public String type() {
        return type.value();
    }

    public boolean isAccepted() {
        return isAccepted;
    }

    public String getPromptText() {
        return promptText;
    }
}
