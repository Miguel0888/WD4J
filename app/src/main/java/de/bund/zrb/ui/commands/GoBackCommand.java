package de.bund.zrb.ui.commands;

import de.bund.zrb.service.BrowserService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class GoBackCommand extends ShortcutMenuCommand {

    private final BrowserService browserService;

    public GoBackCommand(BrowserService browserService) {
        this.browserService = browserService;
    }

    @Override
    public String getId() {
        return "navigation.goback";
    }

    @Override
    public String getLabel() {
        return "Zurück";
    }

    @Override
    public void perform() {
        browserService.goBack();
    }
}
