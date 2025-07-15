package de.bund.zrb.service;

import de.bund.zrb.tools.ScreenshotTool;
import de.bund.zrb.tools.TwoFaTool;

public class ToolsRegistry {

    private static final ToolsRegistry INSTANCE = new ToolsRegistry();

    private final ScreenshotTool screenshotTool;
    private final TwoFaTool twoFaTool;

    private ToolsRegistry() {
        this.screenshotTool = new ScreenshotTool(BrowserServiceImpl.getInstance());
        this.twoFaTool = new TwoFaTool(BrowserServiceImpl.getInstance(), TotpService.getInstance());
    }

    public static ToolsRegistry getInstance() {
        return INSTANCE;
    }

    public ScreenshotTool screenshotTool() {
        return screenshotTool;
    }

    public TwoFaTool twoFaTool() {
        return twoFaTool;
    }
}
