package wd4j.impl.webdriver.type.script;

public class RealmInfo {
    private final String realm;
    private final String type;

    public RealmInfo(String realm, String type) {
        if (realm == null || realm.isEmpty()) {
            throw new IllegalArgumentException("Realm must not be null or empty.");
        }
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Type must not be null or empty.");
        }
        this.realm = realm;
        this.type = type;
    }

    public String getRealm() {
        return realm;
    }

    public String getType() {
        return type;
    }
}