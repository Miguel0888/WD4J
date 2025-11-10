package de.bund.zrb.ui.commands.tools;

import de.bund.zrb.service.ToolsRegistry;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class ChangePasswordCommand extends ShortcutMenuCommand {

    @Override
    public String getId() {
        return "tools.changePassword";
    }

    @Override
    public String getLabel() {
        return "Passwort ändern";
    }

    @Override
    public void perform() {
        ToolsRegistry.getInstance().loginTool().changePasswordForCurrentUser();
    }
}
