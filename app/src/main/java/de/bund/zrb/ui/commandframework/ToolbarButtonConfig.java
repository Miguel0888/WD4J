package de.bund.zrb.ui.commandframework;

/**
 * Describes a single button in a toolbar.
 */
public class ToolbarButtonConfig {

    private final String id;
    private final String iconPath; // Optional
    private final String tooltip;

    public ToolbarButtonConfig(String id, String iconPath, String tooltip) {
        this.id = id;
        this.iconPath = iconPath;
        this.tooltip = tooltip;
    }

    public String getId() {
        return id;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getTooltip() {
        return tooltip;
    }
}
