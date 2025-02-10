package wd4j.impl.webdriver.mapping;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Universeller Adapter für Klassen, die das GenericWrapper-Interface implementieren.
 * Entfernt die generische Verschachtelung und gibt die inneren Werte direkt aus.
 */
public class GenericWrapperAdapter<T extends GenericWrapper> implements JsonSerializer<T>, JsonDeserializer<T> {
    private final Class<T> clazz;

    public GenericWrapperAdapter(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = context.serialize(src).getAsJsonObject();

        // Falls das Objekt ein verschachteltes "value"-Feld enthält, verschiebe dessen Inhalt auf die oberste Ebene
        if (jsonObject.has("value")) {
            JsonObject valueObject = jsonObject.getAsJsonObject("value");
            jsonObject.remove("value"); // Entferne "value"
            for (String key : valueObject.keySet()) { // Verschiebe Felder nach oben
                jsonObject.add(key, valueObject.get(key));
            }
        }

        return jsonObject;
    }

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return context.deserialize(json, clazz);
    }
}
