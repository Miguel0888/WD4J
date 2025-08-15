package de.bund.zrb.ui.commands;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.RecordControlRequestedEvent;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class StopRecordCommand extends ShortcutMenuCommand {

    @Override
    public String getId() {
        return "record.stop";
    }

    @Override
    public String getLabel() {
        return "Aufzeichnung stoppen";
    }

    @Override
    public void perform() {
        // Publish START request; coordinator/tab entscheidet, welche Session betroffen ist.
        ApplicationEventBus.getInstance()
                .publish(new RecordControlRequestedEvent(RecordControlRequestedEvent.RecordOperation.STOP));
    }
}
