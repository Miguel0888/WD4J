package de.bund.zrb.impl.webdriver.type.session;

public class WDUserPromptHandler {
    private final WDUserPromptHandlerType alert; // Optional
    private final WDUserPromptHandlerType beforeUnload; // Optional
    private final WDUserPromptHandlerType confirm; // Optional
    private final WDUserPromptHandlerType defaultHandler; // Optional
    private final WDUserPromptHandlerType prompt; // Optional

    public WDUserPromptHandler(WDUserPromptHandlerType alert, WDUserPromptHandlerType beforeUnload,
                               WDUserPromptHandlerType confirm, WDUserPromptHandlerType defaultHandler,
                               WDUserPromptHandlerType prompt) {
        this.alert = alert;
        this.beforeUnload = beforeUnload;
        this.confirm = confirm;
        this.defaultHandler = defaultHandler;
        this.prompt = prompt;
    }

    public WDUserPromptHandlerType getAlert() {
        return alert;
    }

    public WDUserPromptHandlerType getBeforeUnload() {
        return beforeUnload;
    }

    public WDUserPromptHandlerType getConfirm() {
        return confirm;
    }

    public WDUserPromptHandlerType getDefaultHandler() {
        return defaultHandler;
    }

    public WDUserPromptHandlerType getPrompt() {
        return prompt;
    }
}