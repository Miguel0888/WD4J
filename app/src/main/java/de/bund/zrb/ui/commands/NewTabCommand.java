package de.bund.zrb.ui.commands;

import de.bund.zrb.service.BrowserService;
import de.bund.zrb.ui.commandframework.MenuCommand;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class NewTabCommand extends ShortcutMenuCommand {

    private final BrowserService browserService;

    public NewTabCommand(BrowserService browserService) {
        this.browserService = browserService;
    }

    @Override
    public String getId() {
        return "browser.newtab";
    }

    @Override
    public String getLabel() {
        return "Neuer Tab";
    }

    @Override
    public void perform() {
        browserService.createNewTab();
    }
}
