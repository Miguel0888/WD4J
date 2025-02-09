package wd4j.impl.webdriver.command.request.parameters.input.sourceActions;

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
}
