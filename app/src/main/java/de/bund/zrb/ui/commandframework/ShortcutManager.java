package de.bund.zrb.ui.commandframework;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages keyboard shortcuts mapped to Commands.
 */
public class ShortcutManager {

    private final Map<Integer, String> shortcuts = new HashMap<Integer, String>();
    private final CommandRegistry commandRegistry;

    public ShortcutManager(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    public void registerShortcut(int keyCode, String commandId) {
        shortcuts.put(keyCode, commandId);
    }

    public void handleKeyEvent(KeyEvent event) {
        String commandId = shortcuts.get(event.getKeyCode());
        if (commandId != null) {
            Command command = commandRegistry.getCommand(commandId);
            if (command != null) {
                command.execute(new CommandContext()); // Kontext optional anpassen
            }
        }
    }
}
