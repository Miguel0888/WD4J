package de.bund.zrb.impl.webdriver.command.request.parameters.input.sourceActions;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

@JsonAdapter(NoneSourceAction.NoneSourceActionAdapter.class)
public interface NoneSourceAction {
    /**
     * GSON-Adapter für NoneSourceAction.
     */
    public class NoneSourceActionAdapter implements JsonDeserializer<NoneSourceAction>, JsonSerializer<NoneSourceAction> {

        @Override
        public NoneSourceAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in NoneSourceAction JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "pause":
                    return context.deserialize(jsonObject, PauseAction.class);
                default:
                    throw new JsonParseException("Unknown NoneSourceAction type: " + type);
            }
        }

        @Override
        public JsonElement serialize(NoneSourceAction src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src);
        }
    }

    // --- INTENTIONALLY LEFT EMPTY ---
}
