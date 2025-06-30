package de.bund.zrb.ui.commandframework;

import java.util.Collections;
import java.util.List;

/**
 * Represents a command with menu metadata.
 */
public interface MenuCommand {
    String getId();               // z. B. "file.save"
    String getLabel();            // z. B. "Speichern"
    void perform();
    default List<String> getShortcut() {
        return Collections.emptyList();
    }
    default void setShortcut(List<String> shortcut) {}
}
