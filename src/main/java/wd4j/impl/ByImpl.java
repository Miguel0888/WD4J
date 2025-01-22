package wd4j.api;

public class ByImpl implements By {
    private final String strategy;
    private final String value;

    // Privater Konstruktor, um nur die vorgesehenen Strategien zu ermöglichen
    private ByImpl(String strategy, String value) {
        this.strategy = strategy;
        this.value = value;
    }

    // Getter-Methoden für die Strategie und den Wert
    public String getStrategy() {
        return strategy;
    }

    public String getValue() {
        return value;
    }

    // Statische Methoden für verschiedene Lokalisierungsstrategien

    public static By id(String id) {
        return new ByImpl("css selector", "#" + id); // IDs als CSS-Selektor
    }

    public static By name(String name) {
        return new ByImpl("css selector", "[name='" + name + "']"); // Name-Attribut als CSS
    }

    public static By cssSelector(String cssSelector) {
        return new ByImpl("css selector", cssSelector);
    }

    public static By xpath(String xpathExpression) {
        return new ByImpl("xpath", xpathExpression);
    }

    public static By tagName(String tagName) {
        return new ByImpl("css selector", tagName); // Tag-Namen können als CSS-Selektor verwendet werden
    }

    public static By className(String className) {
        return new ByImpl("css selector", "." + className); // Klassen als CSS-Selektor
    }
}
