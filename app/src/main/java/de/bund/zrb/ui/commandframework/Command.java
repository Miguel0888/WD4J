package de.bund.zrb.ui.commandframework;

/**
 * Represents a generic executable Command.
 */
public interface Command {
    /**
     * Execute the Command.
     *
     * @param context Optional CommandContext
     */
    void execute(CommandContext context);
}
