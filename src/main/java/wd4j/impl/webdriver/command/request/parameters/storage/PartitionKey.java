package wd4j.impl.webdriver.command.request.parameters.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

// ToDo: Maybe a dublicate of the one in wd4j.impl.webdriver.type.storage?
public class PartitionKey {

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
