package de.bund.zrb.impl.webdriver.type.script;

import de.bund.zrb.impl.webdriver.command.request.WDScriptRequest;
import de.bund.zrb.impl.webdriver.mapping.StringWrapper;

/**
 * The script.PreloadScript type represents a handle to a script that will run on realm creation.
 *
 * @see WDScriptRequest.AddPreloadScript
 * @see WDScriptRequest.RemovePreloadScript
 */
public class WDPreloadScript implements StringWrapper {
    private final String value;

    public WDPreloadScript(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty.");
        }
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}