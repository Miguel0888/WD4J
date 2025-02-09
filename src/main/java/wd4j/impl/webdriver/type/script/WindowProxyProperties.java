package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.command.request.BrowsingContextRequest;

public class WindowProxyProperties {
    private final BrowsingContextRequest browsingContextRequest;

    public WindowProxyProperties(BrowsingContextRequest browsingContextRequest) {
        this.browsingContextRequest = browsingContextRequest;
    }

    public BrowsingContextRequest getBrowsingContext() {
        return browsingContextRequest;
    }
}
