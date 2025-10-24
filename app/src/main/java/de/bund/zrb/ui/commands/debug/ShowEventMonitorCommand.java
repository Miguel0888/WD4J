package de.bund.zrb.ui.commands.debug;

import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.debug.WDEventFlagPresets;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.ui.debug.EventMonitorManager;
import de.bund.zrb.ui.debug.EventMonitorWindow;
import de.bund.zrb.websocket.WDEventNames;

import javax.swing.*;
import java.util.EnumMap;

public final class ShowEventMonitorCommand extends ShortcutMenuCommand {
    @Override public String getId()    { return "debug.showEventMonitor"; }
    @Override public String getLabel() { return "Event-Monitor…"; }

    @Override
    public void perform() {
        UserRegistry.User user = UserContextMappingService.getInstance().getCurrentUser();
        if (user == null) {
            JOptionPane.showMessageDialog(null,
                    "Kein aktiver Benutzer gesetzt.\nBitte zuerst einen Benutzer wählen.",
                    "Event-Monitor", JOptionPane.WARNING_MESSAGE);
            return;
        }

        EventMonitorWindow win = EventMonitorManager.getOrCreate(user);

        // Flags aus der Session (falls verfügbar) synchronisieren
        EnumMap<WDEventNames, Boolean> flags = WDEventFlagPresets.recorderDefaults();
        win.setFlags(flags, () -> {
            // Wenn im Fenster Flags geändert werden, hier optional in Session zurückschreiben,
            // falls du eine globale Quelle pflegen willst. (Kann auch leer bleiben.)
        });

        win.setVisible(true);
        win.toFront();
    }
}
