package wd4j.impl.dto.type.script;

import wd4j.impl.dto.mapping.StringWrapper;

/**
 * The script.PreloadScript type represents a handle to a script that will run on realm creation.
 *
 * @see wd4j.impl.dto.command.request.WDScriptRequest.AddPreloadScript
 * @see wd4j.impl.dto.command.request.WDScriptRequest.RemovePreloadScript
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