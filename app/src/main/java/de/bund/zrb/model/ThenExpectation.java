package de.bund.zrb.model;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Repräsentiert eine Erwartung in einem Testfall ("Then"), z. B. Screenshot oder JS-Auswertung.
 * Die konfigurierten Parameter werden als JSON im Feld `value` gespeichert.
 */
public class ThenExpectation {
    private String type;
    private String value;

    private static final Gson gson = new Gson();

    public ThenExpectation() {}

    public ThenExpectation(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }

    public String getValue() { return value; }

    public void setValue(String value) { this.value = value; }

    /**
     * Parsed JSON aus `value` und liefert eine Map<String, Object>.
     */
    public Map<String, Object> getParameterMap() {
        if (value == null || value.trim().isEmpty()) return new HashMap<>();
        try {
            return gson.fromJson(value, new TypeToken<Map<String, Object>>() {}.getType());
        } catch (JsonSyntaxException e) {
            return Collections.emptyMap();
        }
    }

    /**
     * Serialisiert gegebene Parameter-Map in JSON und speichert in `value`.
     */
    public void setParameterMap(Map<String, Object> params) {
        this.value = gson.toJson(params);
    }

    @Override
    public String toString() {
        return "Then[" + type + "]";
    }

    public void validate() throws ValidationException {
        ExpectationTypeDefinition def = de.bund.zrb.model.ExpectationRegistry.getInstance().get(type);
        if (def != null) {
            def.validate(getParameterMap());
        }
    }

}
