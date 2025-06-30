package de.bund.zrb.ui.commands.debug;

import de.bund.zrb.service.BrowserService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class ShowDomEventsCommand extends ShortcutMenuCommand {

    private final BrowserService browserService;
    private boolean enabled = false;

    public ShowDomEventsCommand(BrowserService browserService) {
        this.browserService = browserService;
    }

    @Override
    public String getId() {
        return "debug.showDomEvents";
    }

    @Override
    public String getLabel() {
        return enabled ? "DOM-Events ausblenden" : "DOM-Events anzeigen";
    }

    @Override
    public void perform() {
        enabled = !enabled;
        browserService.showDomEvents(enabled);
    }
}
