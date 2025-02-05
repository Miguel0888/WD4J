package wd4j.impl.webdriver.type.script;

public class Source {
    private final String realm;  // script.Realm
    private final String context; // Optional: browsingContext.BrowsingContext

    public Source(String realm, String context) {
        if (realm == null || realm.isEmpty()) {
            throw new IllegalArgumentException("Realm must not be null or empty.");
        }
        this.realm = realm;
        this.context = context; // Optional, daher keine Prüfung
    }

    public String getRealm() {
        return realm;
    }

    public String getContext() {
        return context;
    }

    @Override
    public String toString() {
        return "ScriptSource{" +
                "realm='" + realm + '\'' +
                ", context='" + context + '\'' +
                '}';
    }
}
