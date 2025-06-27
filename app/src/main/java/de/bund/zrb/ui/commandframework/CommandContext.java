package de.bund.zrb.ui.commandframework;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides optional context data for Command execution.
 */
public class CommandContext {

    private final Map<String, Object> data = new HashMap<String, Object>();

    /**
     * Add a key-value pair to the context.
     *
     * @param key   Context key
     * @param value Value to store
     */
    public void put(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Retrieve a value by key.
     *
     * @param key Context key
     * @return Stored value or null
     */
    public Object get(String key) {
        return data.get(key);
    }

    /**
     * Check if the context contains a given key.
     *
     * @param key Context key
     * @return true if present
     */
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }
}
