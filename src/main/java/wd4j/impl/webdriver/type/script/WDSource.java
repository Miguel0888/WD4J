package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.command.request.WDBrowsingContextRequest;

public class WDSource {
    private final String realm;  // script.Realm
    private final WDBrowsingContextRequest context; // Optional: browsingContext.BrowsingContext

    public WDSource(String realm, WDBrowsingContextRequest context) {
        if (realm == null || realm.isEmpty()) {
            throw new IllegalArgumentException("Realm must not be null or empty.");
        }
        this.realm = realm;
        this.context = context; // Optional, daher keine Prüfung
    }

    public String getRealm() {
        return realm;
    }

    public WDBrowsingContextRequest getContext() {
        return context;
    }
}
