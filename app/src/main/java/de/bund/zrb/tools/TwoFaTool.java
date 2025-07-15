package de.bund.zrb.tools;

import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.TotpService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.components.OtpTestDialog;

import javax.swing.*;
import java.awt.*;

public class TwoFaTool extends AbstractUserTool {

    private final BrowserService browserService;
    private final TotpService totpService;

    public TwoFaTool(BrowserService browserService, TotpService totpService) {
        this.browserService = browserService;
        this.totpService = totpService;
    }

    public String generateSecretFor(UserRegistry.User user) {
        return totpService.generateSecretKey();
    }

    public void showOtpDialog(Window parent) {
        UserRegistry.User user = getCurrentUserOrFail();

        if (user.getOtpSecret() == null || user.getOtpSecret().isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Benutzer hat kein OTP-Secret.", "Hinweis", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        new OtpTestDialog(parent, user).setVisible(true);
    }
}
