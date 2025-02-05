package wd4j.impl.webdriver.type.network;

public class AuthChallenge {
    private final String source;
    private final String origin;

    public AuthChallenge(String source, String origin) {
        this.source = source;
        this.origin = origin;
    }

    public String getSource() {
        return source;
    }

    public String getOrigin() {
        return origin;
    }
}