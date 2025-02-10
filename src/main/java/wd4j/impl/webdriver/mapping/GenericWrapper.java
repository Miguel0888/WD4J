package wd4j.impl.webdriver.mapping;

/**
 * Ein Interface für generische Wrapper-Objekte, die von GSON korrekt serialisiert werden sollen.
 */
public interface GenericWrapper {
    Object value(); // Gibt den serialisierbaren Wert zurück
}
