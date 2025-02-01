package wd4j.impl.module.type;

public class ScriptRealm {
    private final String realm;

    public ScriptRealm(String realm) {
        if (realm == null || realm.isEmpty()) {
            throw new IllegalArgumentException("Realm must not be null or empty.");
        }
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }
}