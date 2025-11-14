package de.bund.zrb.ui.commands.tools;

import de.bund.zrb.service.ToolsRegistry;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class LoginUserCommand extends ShortcutMenuCommand {

    @Override
    public String getId() {
        return "users.loginCurrentUser";
    }

    @Override
    public String getLabel() {
        return "Einloggen";
    }

    @Override
    public void perform() {
        ToolsRegistry.getInstance().loginTool().loginCurrentUser();
    }
}
