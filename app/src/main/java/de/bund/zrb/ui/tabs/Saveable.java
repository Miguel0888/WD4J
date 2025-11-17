package de.bund.zrb.ui.tabs;

/**
 * Markiert einen Editor-Content als speicherbar. Wird vom Save-Revert-Container
 * genutzt und auch vom TabManager beim Wechsel des Preview-Inhalts automatisch
 * aufgerufen.
 */
public interface Saveable {
    /** Persistiert alle aktuellen Änderungen des Editors. */
    void saveChanges();
}

