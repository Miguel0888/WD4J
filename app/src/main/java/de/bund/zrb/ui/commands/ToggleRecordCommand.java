package de.bund.zrb.ui.commands;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.RecordControlRequestedEvent;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class ToggleRecordCommand extends ShortcutMenuCommand {

    @Override
    public String getId() {
        return "record.toggle";
    }

    @Override
    public String getLabel() {
        return "Aufzeichnung starten/stoppen";
    }

    @Override
    public void perform() {
        // Publish START request; coordinator/tab entscheidet, welche Session betroffen ist.
        ApplicationEventBus.getInstance()
                .publish(new RecordControlRequestedEvent(RecordControlRequestedEvent.RecordOperation.TOGGLE));
    }
}
