package wd4j.impl.webdriver.command.request.browsingContext.parameters;

public enum Origin {
    VIEWPORT("viewport"),
    DOCUMENT("document");

    private final String origin;

    Origin(String origin) {
        this.origin = origin;
    }

    public String getOrigin() {
        return origin;
    }
}
