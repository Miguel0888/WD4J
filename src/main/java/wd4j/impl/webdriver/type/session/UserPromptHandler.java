package wd4j.impl.webdriver.type.session;

public class UserPromptHandler {
    private final UserPromptHandlerType alert; // Optional
    private final UserPromptHandlerType beforeUnload; // Optional
    private final UserPromptHandlerType confirm; // Optional
    private final UserPromptHandlerType defaultHandler; // Optional
    private final UserPromptHandlerType prompt; // Optional

    public UserPromptHandler(UserPromptHandlerType alert, UserPromptHandlerType beforeUnload,
                             UserPromptHandlerType confirm, UserPromptHandlerType defaultHandler,
                             UserPromptHandlerType prompt) {
        this.alert = alert;
        this.beforeUnload = beforeUnload;
        this.confirm = confirm;
        this.defaultHandler = defaultHandler;
        this.prompt = prompt;
    }

    public UserPromptHandlerType getAlert() {
        return alert;
    }

    public UserPromptHandlerType getBeforeUnload() {
        return beforeUnload;
    }

    public UserPromptHandlerType getConfirm() {
        return confirm;
    }

    public UserPromptHandlerType getDefaultHandler() {
        return defaultHandler;
    }

    public UserPromptHandlerType getPrompt() {
        return prompt;
    }
}