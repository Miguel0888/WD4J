package de.bund.zrb.support.mapping;

/**
 * Interface für Enums, die als String serialisiert werden sollen.
 *
 *  ACHTUNG: Nicht alle Enums müssen dieses Interface implementieren. Manche sollen den Feldnamen im JSON behalten,
 *  wenn sie den Charakter einer eigenständigen Klasse haben. Das ist allerdings nur regelmäßig dann der Fall, wenn das
 *  Enum keine anderen variablen Felder enthalten soll.
 */
public interface EnumWrapper {
    String value();

    // Java 8 kompatibel: Prüfen, ob ein Wert in einem bestimmten Enum existiert
    static <E extends Enum<E> & EnumWrapper> boolean contains(Class<E> enumClass, String type) {
        for (E e : enumClass.getEnumConstants()) {
            if (e.value().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
