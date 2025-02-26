package wd4j.impl.playwright.event;

import wd4j.api.Dialog;
import wd4j.api.Page;
import wd4j.impl.webdriver.event.WDBrowsingContextEvent;

public class DialogImpl implements Dialog {
    public DialogImpl(WDBrowsingContextEvent.UserPromptClosed userPromptClosed) {
        // TODO: Implement this
    }

    public DialogImpl(WDBrowsingContextEvent.UserPromptOpened userPromptOpened) {
        // TODO: Implement this
    }

    @Override
    public void accept(String promptText) {

    }

    @Override
    public String defaultValue() {
        return "";
    }

    @Override
    public void dismiss() {

    }

    @Override
    public String message() {
        return "";
    }

    @Override
    public Page page() {
        return null;
    }

    @Override
    public String type() {
        return "";
    }
}
