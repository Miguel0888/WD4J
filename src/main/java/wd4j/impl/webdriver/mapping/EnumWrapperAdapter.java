package wd4j.impl.webdriver.mapping;

import com.google.gson.*;

import java.lang.reflect.Type;

public class EnumWrapperAdapter<T extends Enum<T> & EnumWrapper> implements JsonSerializer<T>, JsonDeserializer<T> {
    private final Class<T> clazz;

    @SuppressWarnings("unchecked")
    public EnumWrapperAdapter(Class<? extends EnumWrapper> clazz) {
        this.clazz = (Class<T>) clazz; // Sicheres Casting, weil nur Enums registriert werden
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.value()); // Direkt als String speichern
    }

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String value = json.getAsString();
        for (T constant : clazz.getEnumConstants()) {
            if (constant.value().equals(value)) {
                return constant;
            }
        }
        throw new JsonParseException("Unknown value: " + value + " for enum " + clazz.getSimpleName());
    }
}
