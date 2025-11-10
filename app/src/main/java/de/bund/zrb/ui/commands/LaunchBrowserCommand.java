package de.bund.zrb.ui.commands;

import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class LaunchBrowserCommand extends ShortcutMenuCommand {

    private final BrowserServiceImpl browserService;

    public LaunchBrowserCommand(BrowserServiceImpl browserService) {
        this.browserService = browserService;
    }

    @Override
    public String getId() { return "browser.launch"; }

    @Override
    public String getLabel() { return "Browser starten"; }

    @Override
    public void perform() {
        browserService.launchDefaultBrowser();
    }
}
