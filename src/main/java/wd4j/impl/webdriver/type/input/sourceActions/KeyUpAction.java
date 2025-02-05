package wd4j.impl.webdriver.type.input.sourceActions;

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