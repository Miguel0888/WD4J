package de.bund.zrb.ui.tabs;

/**
 * Markiert einen Editor-Content als verwerfbar. Der Editor stellt seinen
 * Ursprungszustand wieder her und verwirft ungespeicherte Änderungen.
 */
public interface Revertable {
    /** Verwirft ungespeicherte Änderungen und lädt den letzten gespeicherten Stand neu. */
    void revertChanges();
}

