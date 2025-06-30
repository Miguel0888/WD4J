package de.bund.zrb.ui.commands;

import de.bund.zrb.service.BrowserService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class GoForwardCommand extends ShortcutMenuCommand {

    private final BrowserService browserService;

    public GoForwardCommand(BrowserService browserService) {
        this.browserService = browserService;
    }

    @Override
    public String getId() {
        return "navigation.goforward";
    }

    @Override
    public String getLabel() {
        return "Vorwärts";
    }

    @Override
    public void perform() {
        browserService.goForward();
    }
}
