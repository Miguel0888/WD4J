package wd4j.impl.webdriver.type.script;

public class RealmTarget implements Target {
    private final Realm realm;

    public RealmTarget(Realm realm) {
        this.realm = realm;
    }

    public Realm getRealm() {
        return realm;
    }
}
