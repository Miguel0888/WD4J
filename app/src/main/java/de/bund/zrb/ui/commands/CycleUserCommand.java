package de.bund.zrb.ui.commands;

import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.ui.status.StatusBarManager;

public class CycleUserCommand extends ShortcutMenuCommand {
    @Override public String getId()    { return "users.cycle"; }
    @Override public String getLabel() { return "Benutzer wechseln"; }

    @Override
    public void perform() {
        UserRegistry.User newUser = UserContextMappingService.getInstance().cycleNextUser();
        String name = (newUser == null) ? "<Keinen>" : newUser.getUsername();

        StatusBarManager.getInstance().setMessage("Aktiver Benutzer: " + name);
        StatusBarManager.getInstance().setRightText("User: " + name);
    }
}
