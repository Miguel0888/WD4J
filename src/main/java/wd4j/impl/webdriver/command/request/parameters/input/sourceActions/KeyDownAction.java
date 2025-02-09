package wd4j.impl.webdriver.command.request.parameters.input.sourceActions;

public class KeyDownAction extends KeySourceActions {
    private final String value;

    public KeyDownAction(String value) {
        super("keyDown");
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}