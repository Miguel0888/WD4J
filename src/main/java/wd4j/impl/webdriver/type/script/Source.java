package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.command.request.BrowsingContext;

public class Source {
    private final String realm;  // script.Realm
    private final BrowsingContext context; // Optional: browsingContext.BrowsingContext

    public Source(String realm, BrowsingContext context) {
        if (realm == null || realm.isEmpty()) {
            throw new IllegalArgumentException("Realm must not be null or empty.");
        }
        this.realm = realm;
        this.context = context; // Optional, daher keine Prüfung
    }

    public String getRealm() {
        return realm;
    }

    public BrowsingContext getContext() {
        return context;
    }
}
