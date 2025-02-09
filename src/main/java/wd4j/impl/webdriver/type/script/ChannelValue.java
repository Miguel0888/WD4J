package wd4j.impl.webdriver.type.script;

public class ChannelValue {
    private final String type = "channel";
    private final ChannelProperties value;

    public ChannelValue(ChannelProperties value) {
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
        private final Channel channel;
        private final SerializationOptions serializationOptions; // Optional
        private final ResultOwnership ownership; // Optional

        public ChannelProperties(Channel channel) {
            this.channel = channel;
            this.serializationOptions = null;
            this.ownership = null;
        }

        public ChannelProperties(Channel channel, SerializationOptions serializationOptions, ResultOwnership ownership) {
            this.channel = channel;
            this.serializationOptions = serializationOptions;
            this.ownership = ownership;
        }

        public Channel getChannel() {
            return channel;
        }

        public SerializationOptions getSerializationOptions() {
            return serializationOptions;
        }

        public ResultOwnership getOwnership() {
            return ownership;
        }
    }
}