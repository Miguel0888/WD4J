package wd4j.impl.webdriver.command.request.parameters.input.sourceActions;

import wd4j.impl.webdriver.mapping.EnumWrapper;

public class PointerParameters {
    private final PointerType type;

    public PointerParameters() {
        this.type = PointerType.MOUSE;
    }

    public PointerParameters(PointerType type) {
        this.type = type;
    }

    public PointerType getType() {
        return type;
    }

    public enum PointerType implements EnumWrapper {
        MOUSE("mouse"),
        PEN("pen"),
        TOUCH("touch");

        private final String value;

        PointerType(String value) {
            this.value = value;
        }

        @Override // confirmed
        public String value() {
            return value;
        }
    }
}
