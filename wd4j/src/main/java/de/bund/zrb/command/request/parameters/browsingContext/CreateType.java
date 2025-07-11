package de.bund.zrb.command.request.parameters.browsingContext;

import de.bund.zrb.support.mapping.EnumWrapper;

public enum CreateType implements EnumWrapper {
    TAB("tab"),
    WINDOW("window");

    private final String value;

    CreateType(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
