package wd4j.impl.webdriver.command.request.parameters.browsingContext;

public enum CreateType {
    TAB("tab"),
    WINDOW("window");

    private final String type;

    CreateType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
