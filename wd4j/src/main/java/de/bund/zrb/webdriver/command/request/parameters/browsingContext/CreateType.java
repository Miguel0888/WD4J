package de.bund.zrb.webdriver.command.request.parameters.browsingContext;

import de.bund.zrb.webdriver.mapping.EnumWrapper;

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
