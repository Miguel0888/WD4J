package de.bund.zrb.ui.commandframework;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of CommandRegistry.
 */
public class CommandRegistryImpl implements CommandRegistry {

    private final Map<String, Command> commands = new HashMap<String, Command>();

    public void register(String id, Command command) {
        commands.put(id, command);
    }

    public Command getCommand(String id) {
        return commands.get(id);
    }

    public Map<String, Command> getAllCommands() {
        return Collections.unmodifiableMap(commands);
    }
}
