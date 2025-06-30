package de.bund.zrb.ui.commands;

import de.bund.zrb.service.BrowserService;
import de.bund.zrb.ui.commandframework.MenuCommand;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class ReloadTabCommand extends ShortcutMenuCommand {

    private final BrowserService browserService;

    public ReloadTabCommand(BrowserService browserService) {
        this.browserService = browserService;
    }

    @Override
    public String getId() {
        return "navigation.reload";
    }

    @Override
    public String getLabel() {
        return "Neu laden";
    }

    @Override
    public void perform() {
        browserService.reload();
    }
}
