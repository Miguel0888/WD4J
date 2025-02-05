package wd4j.impl.webdriver.type.input;

import com.google.gson.JsonObject;

public class ElementOrigin {

    private final String type;
    private final String elementId;

    /**
     * Constructor for InputOrigin with a specific type.
     *
     * @param type The type of origin (e.g., "viewport", "pointer").
     */
    public ElementOrigin(String type) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Type must not be null or empty.");
        }
        if (!type.equals("viewport") && !type.equals("pointer")) {
            throw new IllegalArgumentException("Invalid type for InputOrigin: " + type);
        }
        this.type = type;
        this.elementId = null;
    }

    /**
     * Constructor for InputOrigin with an element reference.
     *
     * @param elementId The ID of the element to use as the origin.
     */
    public ElementOrigin(String elementId, boolean isElement) {
        if (elementId == null || elementId.isEmpty()) {
            throw new IllegalArgumentException("Element ID must not be null or empty.");
        }
        this.type = "element";
        this.elementId = elementId;
    }

    /**
     * Serializes the InputOrigin into a JSON object.
     *
     * @return A JSON object representing the InputOrigin.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        if ("element".equals(type)) {
            json.addProperty("element", elementId);
        }
        return json;
    }

    // Getters for type and elementId
    public String getType() {
        return type;
    }

    public String getElementId() {
        return elementId;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
