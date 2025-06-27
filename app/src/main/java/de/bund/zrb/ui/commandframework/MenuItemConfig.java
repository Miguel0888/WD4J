package de.bund.zrb.ui.commandframework;

/**
 * Describes a single menu item.
 */
public class MenuItemConfig {

    private final String id;
    private final String label;
    private final String iconPath; // Optional, kann null sein
    private final String shortcut; // Optional, z. B. "ctrl S"

    public MenuItemConfig(String id, String label, String iconPath, String shortcut) {
        this.id = id;
        this.label = label;
        this.iconPath = iconPath;
        this.shortcut = shortcut;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getShortcut() {
        return shortcut;
    }
}
