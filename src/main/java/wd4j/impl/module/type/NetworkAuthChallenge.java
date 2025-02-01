package wd4j.impl.module.type;

public class NetworkAuthChallenge {
    private final String source;
    private final String origin;

    public NetworkAuthChallenge(String source, String origin) {
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