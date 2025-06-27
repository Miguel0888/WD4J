package de.bund.zrb.ui.commandframework;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the configuration for a toolbar.
 */
public class ToolbarConfig {

    private final List<ToolbarButtonConfig> buttons = new ArrayList<ToolbarButtonConfig>();

    public void addButton(ToolbarButtonConfig button) {
        buttons.add(button);
    }

    public List<ToolbarButtonConfig> getButtons() {
        return buttons;
    }
}
