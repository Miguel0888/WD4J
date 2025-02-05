package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;

public class ContextTarget  implements Target {
    private final BrowsingContext context;
    private final String sandbox;

    public ContextTarget(BrowsingContext context, String sandbox) {
        this.context = context;
        this.sandbox = sandbox;
    }

    public BrowsingContext getContext() {
        return context;
    }

    public String getSandbox() {
        return sandbox;
    }
}
