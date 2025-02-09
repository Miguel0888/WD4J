package wd4j.impl.webdriver.command.request.parameters.browsingContext;

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
