package wd4j.impl.dto.mapping;

import com.google.gson.*;

import java.lang.reflect.Type;


public class StringWrapperAdapter<T extends StringWrapper> implements JsonSerializer<T>, JsonDeserializer<T> {
    private final Class<T> clazz;

    public StringWrapperAdapter(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.value()); // Direkt als String speichern
    }

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            return clazz.getConstructor(String.class).newInstance(json.getAsString());
        } catch (Exception e) {
            throw new JsonParseException("Could not deserialize " + clazz.getSimpleName(), e);
        }
    }
}
