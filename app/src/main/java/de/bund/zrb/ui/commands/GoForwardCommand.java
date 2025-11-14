package de.bund.zrb.ui.commands;

import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class GoForwardCommand extends ShortcutMenuCommand {

    private final BrowserService browserService;

    public GoForwardCommand(BrowserService browserService) {
        this.browserService = browserService;
    }

    @Override
    public String getId() {
        return "browser.tab.goforward";
    }

    @Override
    public String getLabel() {
        return "Vorwärts";
    }

    @Override
    public void perform() {
        UserRegistry.User current = UserContextMappingService.getInstance().getCurrentUser();
        if (current != null) {
            browserService.goForward(current.getUsername());
        } else {
            browserService.goForward();
        }
    }

}
