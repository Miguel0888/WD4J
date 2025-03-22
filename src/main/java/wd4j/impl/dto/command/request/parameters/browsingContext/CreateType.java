package wd4j.impl.dto.command.request.parameters.browsingContext;

import wd4j.impl.dto.mapping.EnumWrapper;

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
