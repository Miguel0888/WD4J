package de.bund.zrb.webdriver.type.script;

/**
 * The script.ChannelValue type represents an ArgumentValue that can be deserialized into a function that sends messages
 * from the remote end to the local end.
 */
public class WDChannelValue {
    private final String type = "channel";
    private final ChannelProperties value;

    public WDChannelValue(ChannelProperties value) {
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null or empty.");
        }
        this.value = value;
    }

    public ChannelProperties getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public static class ChannelProperties {
        private final WDChannel channel;
        private final WDSerializationOptions serializationOptions; // Optional
        private final WDResultOwnership ownership; // Optional

        public ChannelProperties(WDChannel channel) {
            this.channel = channel;
            this.serializationOptions = null;
            this.ownership = null;
        }

        public ChannelProperties(WDChannel channel, WDSerializationOptions serializationOptions, WDResultOwnership ownership) {
            this.channel = channel;
            this.serializationOptions = serializationOptions;
            this.ownership = ownership;
        }

        public WDChannel getChannel() {
            return channel;
        }

        public WDSerializationOptions getSerializationOptions() {
            return serializationOptions;
        }

        public WDResultOwnership getOwnership() {
            return ownership;
        }
    }
}