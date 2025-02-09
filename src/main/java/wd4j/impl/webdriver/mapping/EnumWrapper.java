package wd4j.impl.webdriver.mapping;

/**
 * Interface für Enums, die als String serialisiert werden sollen.
 *
 *  ACHTUNG: Nicht alle Enums müssen dieses Interface implementieren. Manche sollen den Feldnamen im JSON behalten,
 *  wenn sie den Charakter einer eigenständigen Klasse haben. Das ist allerdings nur regelmäßig dann der Fall, wenn das
 *  Enum keine anderen variablen Felder enthalten soll.
 */
public interface EnumWrapper {
    String value();
}
