package de.bund.zrb.command.request.parameters.input.sourceActions;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

/**
 * Interface für alle KeySourceActions-Typen.
 */
@JsonAdapter(KeySourceAction.KeySourceActionAdapter.class)
public interface KeySourceAction {
    String getType();

    /**
     * GSON-Adapter für die automatische Serialisierung/Deserialisierung von KeySourceActions.
     */
    class KeySourceActionAdapter implements JsonDeserializer<KeySourceAction>, JsonSerializer<KeySourceAction> {
        @Override
        public KeySourceAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in KeySourceAction JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "pause":
                    return context.deserialize(jsonObject, PauseAction.class);
                case "keyDown":
                    return context.deserialize(jsonObject, KeyDownAction.class);
                case "keyUp":
                    return context.deserialize(jsonObject, KeyUpAction.class);
                default:
                    throw new JsonParseException("Unknown KeySourceAction type: " + type);
            }
        }

        @Override
        public JsonElement serialize(KeySourceAction src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src);
        }
    }

    /**
     * Repräsentiert eine "keyDown"-Aktion.
     */
    class KeyDownAction implements KeySourceAction {
        private final String type = "keyDown";
        private final String value;

        public KeyDownAction(String value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Repräsentiert eine "keyUp"-Aktion.
     */
    class KeyUpAction implements KeySourceAction {
        private final String type = "keyUp";
        private final String value;

        public KeyUpAction(String value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }
}
