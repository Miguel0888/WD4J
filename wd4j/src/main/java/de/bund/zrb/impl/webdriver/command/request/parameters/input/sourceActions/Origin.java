package de.bund.zrb.impl.webdriver.command.request.parameters.input.sourceActions;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import de.bund.zrb.impl.webdriver.mapping.EnumWrapper;
import de.bund.zrb.impl.webdriver.type.input.WDElementOrigin;
import java.lang.reflect.Type;

/**
 * Interface für die möglichen Ursprünge einer Wheel- oder Pointer-Action.
 */
@JsonAdapter(Origin.OriginAdapter.class)
public interface Origin {

    /**
     * GSON-Adapter für die automatische Serialisierung/Deserialisierung von Origin.
     */
    class OriginAdapter implements JsonDeserializer<Origin>, JsonSerializer<Origin> {
        @Override
        public Origin deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                String type = json.getAsString();
                switch (type) {
                    case "viewport":
                        return FixedOrigin.VIEWPORT;
                    case "pointer":
                        return FixedOrigin.POINTER;
                    default:
                        throw new JsonParseException("Unknown fixed origin: " + type);
                }
            } else if (json.isJsonObject()) {
                return context.deserialize(json, WDElementOrigin.class);
            }
            throw new JsonParseException("Invalid Origin format");
        }

        @Override
        public JsonElement serialize(Origin src, Type typeOfSrc, JsonSerializationContext context) {
            if (src instanceof FixedOrigin) {
                return new JsonPrimitive(((FixedOrigin) src).value);
            }
            return context.serialize(src);
        }
    }

    /**
     * Feste Ursprünge als Enum (VIEWPORT, POINTER).
     */
    enum FixedOrigin implements Origin, EnumWrapper {
        VIEWPORT("viewport"),
        POINTER("pointer");

        private final String value;

        FixedOrigin(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
