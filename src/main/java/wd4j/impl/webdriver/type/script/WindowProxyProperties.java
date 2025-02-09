package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.command.request.BrowsingContext;

public class WindowProxyProperties {
    private final BrowsingContext browsingContext;

    public WindowProxyProperties(BrowsingContext browsingContext) {
        this.browsingContext = browsingContext;
    }

    public BrowsingContext getBrowsingContext() {
        return browsingContext;
    }
}
