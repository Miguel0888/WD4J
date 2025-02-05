package wd4j.impl.webdriver.type.browsingContext;

import com.google.gson.JsonObject;

public class Locator {
    private final String type;
    private final JsonObject value;

    private Locator(String type, JsonObject value) {
        this.type = type;
        this.value = value;
    }

    public static Locator css(String selector) {
        JsonObject value = new JsonObject();
        value.addProperty("value", selector);
        return new Locator("css", value);
    }

    public static Locator xpath(String expression) {
        JsonObject value = new JsonObject();
        value.addProperty("value", expression);
        return new Locator("xpath", value);
    }

    public static Locator innerText(String text, boolean ignoreCase, String matchType) {
        JsonObject value = new JsonObject();
        value.addProperty("value", text);
        value.addProperty("ignoreCase", ignoreCase);
        value.addProperty("matchType", matchType);
        return new Locator("innerText", value);
    }

    public static Locator accessibility(String name, String role) {
        JsonObject value = new JsonObject();
        if (name != null) value.addProperty("name", name);
        if (role != null) value.addProperty("role", role);
        return new Locator("accessibility", value);
    }

    public static Locator context(String contextId) {
        JsonObject value = new JsonObject();
        value.addProperty("context", contextId);
        return new Locator("context", value);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", this.type);
        json.add("value", this.value);
        return json;
    }
}
