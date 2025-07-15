package de.bund.zrb.ui.commands.tools;

import de.bund.zrb.PageImpl;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.ui.components.OtpTestDialog;

import javax.swing.*;
import java.awt.*;

public class ShowOtpDialogCommand extends ShortcutMenuCommand {

    @Override
    public String getId() {
        return "tools.showOtpForActiveUser";
    }

    @Override
    public String getLabel() {
        return "OTP-Code (aktiver Benutzer)";
    }

    @Override
    public void perform() {
        UserRegistry.User user = UserContextMappingService.getInstance().getCurrentUser();

        if (user == null) {
            JOptionPane.showMessageDialog(null, "Kein Benutzer für aktiven Kontext gefunden.", "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (user.getOtpSecret() == null || user.getOtpSecret().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Benutzer hat kein OTP-Secret.", "Hinweis", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Window parent = getActiveWindow();
        new OtpTestDialog(parent, user).setVisible(true);
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
