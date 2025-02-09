package wd4j.impl.webdriver.type.browsingContext.locator;

import wd4j.impl.webdriver.type.browsingContext.Locator;

public class CssLocator implements Locator<String> {
    private final String type = "css";
    private final String value;

    public CssLocator(String value) {
        this.value = value;
    }


    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getValue() {
        return value;
    }
}
