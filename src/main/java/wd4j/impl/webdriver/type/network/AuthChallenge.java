package wd4j.impl.webdriver.type.network;

public class AuthChallenge {
    private final String scheme;
    private final String realm;

    public AuthChallenge(String scheme, String realm) {
        this.scheme = scheme;
        this.realm = realm;
    }

    public String getScheme() {
        return scheme;
    }

    public String getRealm() {
        return realm;
    }
}