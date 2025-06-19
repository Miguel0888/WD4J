package de.bund.zrb.webdriver.type.script;

import de.bund.zrb.webdriver.type.browsingContext.WDBrowsingContext;

/**
 * The script.Source type represents a script.Realm with an optional browsingContext.BrowsingContext in which a script
 * related event occurred.
 */
public class WDSource {
    private final String realm;  // script.Realm
    private final WDBrowsingContext context; // Optional: browsingContext.BrowsingContext

    public WDSource(String realm, WDBrowsingContext context) {
        if (realm == null || realm.isEmpty()) {
            throw new IllegalArgumentException("Realm must not be null or empty.");
        }
        this.realm = realm;
        this.context = context; // Optional, daher keine Prüfung
    }

    public String getRealm() {
        return realm;
    }

    public WDBrowsingContext getContext() {
        return context;
    }
}
