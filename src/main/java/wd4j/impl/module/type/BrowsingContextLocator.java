package wd4j.impl.module.type;

import com.google.gson.JsonObject;

public class BrowsingContextLocator {
    private final String type;
    private final JsonObject value;

    private BrowsingContextLocator(String type, JsonObject value) {
        this.type = type;
        this.value = value;
    }

    public static BrowsingContextLocator css(String selector) {
        JsonObject value = new JsonObject();
        value.addProperty("value", selector);
        return new BrowsingContextLocator("css", value);
    }

    public static BrowsingContextLocator xpath(String expression) {
        JsonObject value = new JsonObject();
        value.addProperty("value", expression);
        return new BrowsingContextLocator("xpath", value);
    }

    public static BrowsingContextLocator innerText(String text, boolean ignoreCase, String matchType) {
        JsonObject value = new JsonObject();
        value.addProperty("value", text);
        value.addProperty("ignoreCase", ignoreCase);
        value.addProperty("matchType", matchType);
        return new BrowsingContextLocator("innerText", value);
    }

    public static BrowsingContextLocator accessibility(String name, String role) {
        JsonObject value = new JsonObject();
        if (name != null) value.addProperty("name", name);
        if (role != null) value.addProperty("role", role);
        return new BrowsingContextLocator("accessibility", value);
    }

    public static BrowsingContextLocator context(String contextId) {
        JsonObject value = new JsonObject();
        value.addProperty("context", contextId);
        return new BrowsingContextLocator("context", value);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", this.type);
        json.add("value", this.value);
        return json;
    }
}
