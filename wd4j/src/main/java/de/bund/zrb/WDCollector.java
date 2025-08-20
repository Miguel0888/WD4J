package de.bund.zrb.type.network;

import de.bund.zrb.support.mapping.StringWrapper;

/**
 * Represent the WebDriver BiDi network.Collector identifier.
 * Serialize as a plain JSON string.
 */
public class WDCollector implements StringWrapper {
    private final String value;

    public WDCollector(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty.");
        }
        this.value = id;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
