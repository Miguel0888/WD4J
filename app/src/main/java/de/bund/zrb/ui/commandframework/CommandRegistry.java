package de.bund.zrb.ui.commandframework;

import java.util.Map;

/**
 * Registry for registering and retrieving Commands.
 */
public interface CommandRegistry {

    void register(MenuCommand menuCommand);
}
