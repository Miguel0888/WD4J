package wd4j.impl.webdriver.type.input;

public abstract class SourceActions {
    private String type;

    public SourceActions(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

}
