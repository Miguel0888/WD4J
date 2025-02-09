package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;

public abstract class Target {
}

class RealmTarget extends Target {
    private final Realm realm;

    public RealmTarget(Realm realm) {
        this.realm = realm;
    }

    public Realm getRealm() {
        return realm;
    }
}

class ContextTarget extends Target {
    private final BrowsingContext context;
    private final String sandbox; // Optional

    public ContextTarget(BrowsingContext context) {
        this(context, null);
    }

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
