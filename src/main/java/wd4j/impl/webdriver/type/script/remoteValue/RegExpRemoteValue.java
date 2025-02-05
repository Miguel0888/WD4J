package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;

public class RegExpRemoteValue extends RemoteValue {
    private final String pattern;
    private final String flags;

    public RegExpRemoteValue(String handle, String internalId, String pattern, String flags) {
        super("regexp", handle, internalId);
        this.pattern = pattern;
        this.flags = flags;
    }

    public String getPattern() {
        return pattern;
    }

    public String getFlags() {
        return flags;
    }

    @Override
    public String toString() {
        return "RegExpRemoteValue{" +
                "type='" + getType() + '\'' +
                ", handle='" + getHandle() + '\'' +
                ", internalId='" + getInternalId() + '\'' +
                ", pattern='" + pattern + '\'' +
                ", flags='" + flags + '\'' +
                '}';
    }
}
