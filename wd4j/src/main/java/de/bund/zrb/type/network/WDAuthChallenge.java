package de.bund.zrb.type.network;

public class WDAuthChallenge {
    private final String scheme;
    private final String realm;

    public WDAuthChallenge(String scheme, String realm) {
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