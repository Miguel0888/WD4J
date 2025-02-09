package wd4j.impl.webdriver.type.input.parameters.sourceActions;

import wd4j.impl.webdriver.type.input.ElementOrigin;

// ToDo: Maybe this is ElementOrigin instead of Origin?
public class Origin {
    private final String origin;
    private final ElementOrigin elementOrigin;

    public Origin( String origin ) {
        this.origin = origin;
        this.elementOrigin = null;
    }

    public Origin( ElementOrigin elementOrigin)
    {
        this.elementOrigin = elementOrigin;
        this.origin = null;
    }

    // ToDo: How to return the origin?
}
