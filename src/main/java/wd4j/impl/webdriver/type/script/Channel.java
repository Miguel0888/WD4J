package wd4j.impl.webdriver.type.script;

public class Channel {
    private final String channel;

    public Channel(String channel) {
        if (channel == null || channel.isEmpty()) {
            throw new IllegalArgumentException("Channel must not be null or empty.");
        }
        this.channel = channel;
    }

    public String getChannel() {
        return channel;
    }
}