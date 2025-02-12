package wd4j.impl.webdriver.command.request.parameters.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import wd4j.impl.webdriver.type.network.BytesValue;
import wd4j.impl.webdriver.type.network.SameSite;
import wd4j.impl.websocket.Command;

public class SetCookieParameters implements Command.Params {
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
        private BytesValue value;
        private String domain;
        private String path;
        private boolean httpOnly;
        private boolean secure;
        private SameSite sameSite;
        private int expiry;

        public PartialCookie(String name, BytesValue value, String domain, String path, boolean httpOnly, boolean secure, SameSite sameSite, int expiry) {
            this.name = name;
            this.value = value;
            this.domain = domain;
            this.path = path;
            this.httpOnly = httpOnly;
            this.secure = secure;
            this.sameSite = sameSite;
            this.expiry = expiry;
        }

        public String getName() {
            return name;
        }

        public BytesValue getValue() {
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

        public SameSite getSameSite() {
            return sameSite;
        }

        public int getExpiry() {
            return expiry;
        }
    }
}
