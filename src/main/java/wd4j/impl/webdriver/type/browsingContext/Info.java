package wd4j.impl.webdriver.type.browsingContext;

import wd4j.impl.webdriver.type.browser.ClientWindow;
import wd4j.impl.webdriver.type.browser.UserContext;

import java.util.List;

public class Info {
    private final List<Info> children;
    private final ClientWindow clientWindow;
    private final BrowsingContext context;
    private final BrowsingContext originalOpener;
    private final String url;
    private final UserContext userContext;
    private final BrowsingContext parent; // optional

    public Info(List<Info> children, ClientWindow clientWindow, BrowsingContext context, BrowsingContext originalOpener, String url, UserContext userContext, BrowsingContext parent) {
        this.children = children;
        this.clientWindow = clientWindow;
        this.context = context;
        this.originalOpener = originalOpener;
        this.url = url;
        this.userContext = userContext;
        this.parent = parent;
    }

    public List<Info> getChildren() {
        return children;
    }

    public ClientWindow getClientWindow() {
        return clientWindow;
    }

    public BrowsingContext getContext() {
        return context;
    }

    public BrowsingContext getOriginalOpener() {
        return originalOpener;
    }

    public String getUrl() {
        return url;
    }

    public UserContext getUserContext() {
        return userContext;
    }

    public BrowsingContext getParent() {
        return parent;
    }
}