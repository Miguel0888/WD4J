package de.bund.zrb.service;

import de.bund.zrb.tools.*;

public class ToolsRegistry {

    private static final ToolsRegistry INSTANCE = new ToolsRegistry();

    private final ScreenshotTool screenshotTool;
    private final TwoFaTool twoFaTool;
    private final NavigationTool navigationTool;
    private final LoginTool loginTool;
    private final NotificationTool notificationTool;

    public ToolsRegistry() {
        BrowserService browserService = BrowserServiceImpl.getInstance();
        this.screenshotTool = new ScreenshotTool(browserService);
        this.twoFaTool = new TwoFaTool(browserService, TotpService.getInstance());
        this.navigationTool = new NavigationTool(browserService);
        this.loginTool = new LoginTool(BrowserServiceImpl.getInstance(), TotpService.getInstance());
        this.notificationTool = new NotificationTool(browserService); // <— NEU
    }

    public static ToolsRegistry getInstance() {
        return INSTANCE;
    }

    public ScreenshotTool screenshotTool() { return screenshotTool; }
    public TwoFaTool twoFaTool()           { return twoFaTool; }
    public NavigationTool navigationTool() { return navigationTool; }
    public LoginTool loginTool()           { return loginTool; }
    public NotificationTool notificationTool() { return notificationTool; }
}
