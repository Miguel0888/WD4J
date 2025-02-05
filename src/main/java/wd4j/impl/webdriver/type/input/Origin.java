package wd4j.impl.webdriver.type.input;

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
