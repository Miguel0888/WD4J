package wd4j.impl.webdriver.type.script;

public class EvaluateResult {
    private String type;
    private Realm realm;

    public EvaluateResult(String type, Realm realm) {
        this.type = type;
        this.realm = realm;
    }

    public String getType() {
        return type;
    }

    public Realm getRealm() {
        return realm;
    }
}