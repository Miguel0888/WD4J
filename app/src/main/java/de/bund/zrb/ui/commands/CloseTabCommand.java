package de.bund.zrb.ui.commands;


import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.commandframework.MenuCommand;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class CloseTabCommand extends ShortcutMenuCommand {

    private final BrowserService browserService;

    public CloseTabCommand(BrowserService browserService) {
        this.browserService = browserService;
    }

    @Override
    public String getId() {
        return "browser.closetab";
    }

    @Override
    public String getLabel() {
        return "Tab schließen";
    }

    @Override
    public void perform() {
        UserRegistry.User current = UserContextMappingService.getInstance().getCurrentUser();
        if (current != null) {
            browserService.closeActiveTab(current.getUsername());
        } else {
            browserService.closeActiveTab();
        }
    }

}
