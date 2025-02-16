package wd4j.impl.webdriver.type.script;

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
        private final WDChannel WDChannel;
        private final WDSerializationOptions WDSerializationOptions; // Optional
        private final WDResultOwnership ownership; // Optional

        public ChannelProperties(WDChannel WDChannel) {
            this.WDChannel = WDChannel;
            this.WDSerializationOptions = null;
            this.ownership = null;
        }

        public ChannelProperties(WDChannel WDChannel, WDSerializationOptions WDSerializationOptions, WDResultOwnership ownership) {
            this.WDChannel = WDChannel;
            this.WDSerializationOptions = WDSerializationOptions;
            this.ownership = ownership;
        }

        public WDChannel getChannel() {
            return WDChannel;
        }

        public WDSerializationOptions getSerializationOptions() {
            return WDSerializationOptions;
        }

        public WDResultOwnership getOwnership() {
            return ownership;
        }
    }
}