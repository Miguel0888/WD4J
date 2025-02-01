package wd4j.impl.module.type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class StoragePartitionKey {

    private final String type;
    private final String topLevelOrigin;
    private final boolean isNull;

    /**
     * Constructor for a null PartitionKey.
     */
    public StoragePartitionKey() {
        this.type = null;
        this.topLevelOrigin = null;
        this.isNull = true;
    }

    /**
     * Constructor for a PartitionKey with a top-level origin.
     *
     * @param topLevelOrigin The top-level origin for the partition key.
     */
    public StoragePartitionKey(String topLevelOrigin) {
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
