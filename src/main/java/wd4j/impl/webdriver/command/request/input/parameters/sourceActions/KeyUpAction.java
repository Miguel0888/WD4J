package wd4j.impl.webdriver.command.request.input.parameters.sourceActions;

public class KeyUpAction extends KeySourceActions {
    private final String value;

    public KeyUpAction(String value) {
        super("keyUp");
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}