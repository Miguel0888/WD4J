package wd4j.impl.webdriver.type.network;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

@JsonAdapter(BytesValue.BytesValueAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface BytesValue {
    String getType();
    String getValue();

    // 🔥 **INNERE KLASSE für JSON-Deserialisierung**
    class BytesValueAdapter implements JsonDeserializer<BytesValue> {
        @Override
        public BytesValue deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in BytesValue JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "string":
                    return context.deserialize(jsonObject, StringValue.class);
                default:
                    throw new JsonParseException("Unknown BytesValue type: " + type);
            }
        }
    }

    class StringValue implements BytesValue {
        private final String type = "string";
        private final String value;

        public StringValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public String getType() {
            return type;
        }
    }
}