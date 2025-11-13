package de.bund.zrb.ui.settings;

import javax.swing.*;
import java.util.Map;

/** Gemeinsame Schnittstelle für alle Settings-Unterpanels. */
public interface SettingsSubPanel {
    /** Eindeutige ID für CardLayout. */
    String getId();
    /** Titel für linke Navigation. */
    String getTitle();
    /** Panel-Komponente. */
    JComponent getComponent();
    /** Werte aus Settings lesen und ins UI übernehmen. */
    void loadFromSettings();
    /** Werte validieren und in die Map schreiben (Key→Value). */
    void putTo(Map<String,Object> out) throws IllegalArgumentException;
}

