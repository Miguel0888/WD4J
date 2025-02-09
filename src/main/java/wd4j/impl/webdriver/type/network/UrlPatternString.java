package wd4j.impl.webdriver.type.network;

public class UrlPatternString implements UrlPattern {
    private final String type = "string";
    private final String pattern;

    public UrlPatternString(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String getType() {
        return type;
    }

    public String getPattern() {
        return pattern;
    }
}
