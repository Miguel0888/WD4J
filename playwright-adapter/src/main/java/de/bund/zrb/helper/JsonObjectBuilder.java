package de.bund.zrb.helper;

import com.google.gson.JsonObject;

public class JsonObjectBuilder {
    private final JsonObject jsonObject;

    public JsonObjectBuilder() {
        this.jsonObject = new JsonObject();
    }

    public JsonObjectBuilder addProperty(String key, String value) {
        jsonObject.addProperty(key, value);
        return this;
    }

    public JsonObjectBuilder addProperty(String key, Number value) {
        jsonObject.addProperty(key, value);
        return this;
    }

    public JsonObjectBuilder add(String key, JsonObject value) {
        jsonObject.add(key, value);
        return this;
    }

    public JsonObject build() {
        return jsonObject;
    }
}
