package de.bund.zrb.ui.commands;

import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.service.UserRegistry;
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
        UserRegistry.User current = UserContextMappingService.getInstance().getCurrentUser();
        if (current != null) {
            browserService.reload(current.getUsername());
        } else {
            browserService.reload();
        }
    }

}
