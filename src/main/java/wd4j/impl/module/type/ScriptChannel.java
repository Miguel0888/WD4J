package wd4j.impl.module.type;

public class ScriptChannel {
    private final String channel;

    public ScriptChannel(String channel) {
        if (channel == null || channel.isEmpty()) {
            throw new IllegalArgumentException("Channel must not be null or empty.");
        }
        this.channel = channel;
    }

    public String getChannel() {
        return channel;
    }
}