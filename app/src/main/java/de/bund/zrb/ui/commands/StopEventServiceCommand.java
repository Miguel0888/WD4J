package de.bund.zrb.ui.commands;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.EventServiceControlRequestedEvent;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

/**
 * Command to stop event logging for the currently active recorder tab. Invoking this
 * command will publish an {@link EventServiceControlRequestedEvent} with operation
 * {@link EventServiceControlRequestedEvent.Operation#STOP} and no specific user,
 * thereby requesting the {@link de.bund.zrb.service.RecorderCoordinator} to stop
 * event logging on the active tab.
 */
public class StopEventServiceCommand extends ShortcutMenuCommand {

    @Override
    public String getId() {
        return "debug.stop";
    }

    @Override
    public String getLabel() {
        return "Event-Logging stoppen";
    }

    @Override
    public void perform() {
        ApplicationEventBus.getInstance().publish(
                new EventServiceControlRequestedEvent(EventServiceControlRequestedEvent.Operation.STOP)
        );
    }
}