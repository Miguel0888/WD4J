package de.bund.zrb.ui.commands;

import de.bund.zrb.service.BrowserConfig;
import de.bund.zrb.service.BrowserService;
import de.bund.zrb.ui.commandframework.MenuCommand;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class LaunchBrowserCommand extends ShortcutMenuCommand {

    private final BrowserService browserService;

    public LaunchBrowserCommand(BrowserService browserService) {
        this.browserService = browserService;
    }

    @Override
    public String getId() {
        return "browser.launch";
    }

    @Override
    public String getLabel() {
        return "Browser starten";
    }

    @Override
    public void perform() {
        BrowserConfig config = new BrowserConfig();
        config.setBrowserType("firefox");
        config.setHeadless(false);
        config.setNoRemote(false);
        config.setDisableGpu(false);
        config.setStartMaximized(true);
        config.setUseProfile(false);
        config.setPort(9222);

        browserService.launchBrowser(config);
    }
}
