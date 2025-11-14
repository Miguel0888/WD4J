package de.bund.zrb.ui.commands.tools;

import de.bund.zrb.service.ToolsRegistry;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

import java.awt.*;

public class ShowOtpDialogCommand extends ShortcutMenuCommand {

    @Override
    public String getId() {
        return "users.showOtpForActiveUser";
    }

    @Override
    public String getLabel() {
        return "OTP-Code anzeigen";
    }

    @Override
    public void perform() {
        ToolsRegistry.getInstance().twoFaTool().showOtpDialog(getActiveWindow());
    }

    private Window getActiveWindow() {
        for (Window w : Window.getWindows()) {
            if (w.isActive()) {
                return w;
            }
        }
        return null;
    }
}
