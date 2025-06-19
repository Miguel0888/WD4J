package de.bund.zrb.impl.webdriver.command.request.parameters.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.bund.zrb.impl.webdriver.type.network.WDBytesValue;
import de.bund.zrb.impl.webdriver.type.network.WDSameSite;
import de.bund.zrb.impl.websocket.WDCommand;

public class SetCookieParameters implements WDCommand.Params {
    private final PartialCookie cookie;
    private final PartitionDescriptor partition; // Optional

    public SetCookieParameters(PartialCookie cookie) {
        this(cookie, null);
    }

    public SetCookieParameters(PartialCookie cookie, PartitionDescriptor partition) {
        this.cookie = cookie;
        this.partition = partition;
    }

    public PartialCookie getCookie() {
        return cookie;
    }

    public PartitionDescriptor getPartition() {
        return partition;
    }

    // ToDo: Maybe a dublicate of the one in wd4j.impl.webdriver.type.storage?
    public static class PartitionKey {

        private final String type;
        private final String topLevelOrigin;
        private final boolean isNull;

        /**
         * Constructor for a null PartitionKey.
         */
        public PartitionKey() {
            this.type = null;
            this.topLevelOrigin = null;
            this.isNull = true;
        }

        /**
         * Constructor for a PartitionKey with a top-level origin.
         *
         * @param topLevelOrigin The top-level origin for the partition key.
         */
        public PartitionKey(String topLevelOrigin) {
            if (topLevelOrigin == null || topLevelOrigin.isEmpty()) {
                throw new IllegalArgumentException("Top-level origin must not be null or empty.");
            }
            this.type = "origin";
            this.topLevelOrigin = topLevelOrigin;
            this.isNull = false;
        }

        /**
         * Serializes the PartitionKey into a JSON object.
         *
         * @return A JSON representation of the PartitionKey.
         */
        public JsonElement toJson() {
            if (isNull) {
                return null; // Represents a null PartitionKey
            }

            JsonObject json = new JsonObject();
            json.addProperty("type", type);
            if ("origin".equals(type)) {
                json.addProperty("topLevelOrigin", topLevelOrigin);
            }
            return json;
        }

        @Override
        public String toString() {
            JsonElement json = toJson();
            return json != null ? json.toString() : "null";
        }
    }

    public static class PartialCookie {

        private String name;
        private WDBytesValue value;
        private String domain;
        private String path;
        private boolean httpOnly;
        private boolean secure;
        private WDSameSite WDSameSite;
        private int expiry;

        public PartialCookie(String name, WDBytesValue value, String domain, String path, boolean httpOnly, boolean secure, WDSameSite WDSameSite, int expiry) {
            this.name = name;
            this.value = value;
            this.domain = domain;
            this.path = path;
            this.httpOnly = httpOnly;
            this.secure = secure;
            this.WDSameSite = WDSameSite;
            this.expiry = expiry;
        }

        public String getName() {
            return name;
        }

        public WDBytesValue getValue() {
            return value;
        }

        public String getDomain() {
            return domain;
        }

        public String getPath() {
            return path;
        }

        public boolean isHttpOnly() {
            return httpOnly;
        }

        public boolean isSecure() {
            return secure;
        }

        public WDSameSite getSameSite() {
            return WDSameSite;
        }

        public int getExpiry() {
            return expiry;
        }
    }
}
