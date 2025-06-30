package de.bund.zrb.ui.commands.debug;

import de.bund.zrb.service.BrowserService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class ShowSelectorsCommand extends ShortcutMenuCommand {

    private final BrowserService browserService;
    private boolean enabled = false;

    public ShowSelectorsCommand(BrowserService browserService) {
        this.browserService = browserService;
    }

    @Override
    public String getId() {
        return "debug.showSelectors";
    }

    @Override
    public String getLabel() {
        return enabled ? "Selector-Overlay ausblenden" : "Selector-Overlay anzeigen";
    }

    @Override
    public void perform() {
        enabled = !enabled;
        browserService.showSelectors(enabled);
    }
}
