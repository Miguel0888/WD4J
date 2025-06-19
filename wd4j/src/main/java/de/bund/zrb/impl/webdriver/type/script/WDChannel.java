package de.bund.zrb.impl.webdriver.type.script;

import de.bund.zrb.impl.webdriver.mapping.StringWrapper;

/**
 * The script.Channel type represents the id of a specific channel used to send custom messages from the remote end to the local end.
 */
public class WDChannel implements StringWrapper {
    private final String value;

    public WDChannel(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Channel must not be null or empty.");
        }
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}