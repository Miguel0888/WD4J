package wd4j.impl.webdriver.type.script;

public class PreloadScript {
    private final String id;
    private final String functionDeclaration;

    public PreloadScript(String id, String functionDeclaration) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty.");
        }
        if (functionDeclaration == null || functionDeclaration.isEmpty()) {
            throw new IllegalArgumentException("Function declaration must not be null or empty.");
        }
        this.id = id;
        this.functionDeclaration = functionDeclaration;
    }

    public String getId() {
        return id;
    }

    public String getFunctionDeclaration() {
        return functionDeclaration;
    }
}