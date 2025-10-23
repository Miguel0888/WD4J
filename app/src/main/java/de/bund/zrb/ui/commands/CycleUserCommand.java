// File: app/src/main/java/de/bund/zrb/ui/commands/CycleUserCommand.java
package de.bund.zrb.ui.commands;

import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class CycleUserCommand extends ShortcutMenuCommand {
    @Override public String getId()    { return "users.cycle"; }
    @Override public String getLabel() { return "Benutzer wechseln"; }

    @Override
    public void perform() {
        // Weiterschalten im Service; UserSelectionCombo hört auf "currentUser" und setzt sich selbst
        UserContextMappingService.getInstance().cycleNextUser();
    }
}
