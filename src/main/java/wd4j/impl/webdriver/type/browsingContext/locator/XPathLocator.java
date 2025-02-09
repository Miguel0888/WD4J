package wd4j.impl.webdriver.type.browsingContext.locator;

import wd4j.impl.webdriver.type.browsingContext.Locator;

public class XPathLocator implements Locator<String> {
    private final String type = "xpath";
    private final String value;

    public XPathLocator(String value) {
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
