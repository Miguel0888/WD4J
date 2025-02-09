package wd4j.impl.webdriver.type.script;

// ToDo: Not directly defined as Type, but used by events and parameters
public class Realm {
    private final String realm;

    public Realm(String realm) {
        if (realm == null || realm.isEmpty()) {
            throw new IllegalArgumentException("Realm must not be null or empty.");
        }
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }
}