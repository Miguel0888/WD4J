package de.bund.zrb.ui.commands;

import de.bund.zrb.service.BrowserService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class TerminateBrowserCommand extends ShortcutMenuCommand {

    private final BrowserService browserService;

    public TerminateBrowserCommand(BrowserService browserService) {
        this.browserService = browserService;
    }

    @Override
    public String getId() {
        return "browser.terminate";
    }

    @Override
    public String getLabel() {
        return "Browser beenden";
    }

    @Override
    public void perform() {
        browserService.terminateBrowser();
    }
}
