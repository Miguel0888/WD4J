package de.bund.zrb.ui.commandframework;

import java.util.Map;

/**
 * Registry for registering and retrieving Commands.
 */
public interface CommandRegistry {

    /**
     * Register a Command with a unique identifier.
     *
     * @param id Command ID
     * @param command Command instance
     */
    void register(String id, Command command);

    /**
     * Retrieve a Command by ID.
     *
     * @param id Command ID
     * @return Command instance
     */
    Command getCommand(String id);

    /**
     * @return All registered Commands.
     */
    Map<String, Command> getAllCommands();
}
