package de.bund.zrb.tools;

import com.microsoft.playwright.Page;
import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.TotpService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.tools.AbstractUserTool;

public class LoginTool extends AbstractUserTool {

    private final BrowserService browserService;
    private final TotpService totpService;

    public LoginTool(BrowserService browserService, TotpService totpService) {
        this.browserService = browserService;
        this.totpService = totpService;
    }

    public void loginCurrentUser() {
        UserRegistry.User user = getCurrentUserOrFail();
        login(user);
    }

    public void login(UserRegistry.User user) {
        String loginUrl = user.getLoginPage();
        if (loginUrl == null || loginUrl.trim().isEmpty()) {
            throw new IllegalStateException("Login-Seite nicht definiert für Benutzer: " + user.getUsername());
        }

        Page page = browserService.getActivePage(user.getUsername());
        page.navigate(loginUrl);

        UserRegistry.User.LoginConfig config = user.getLoginConfig();

        if (config.getUsernameSelector() == null ||
                config.getPasswordSelector() == null ||
                config.getSubmitSelector() == null) {
            throw new IllegalStateException("Login-Konfiguration unvollständig für Benutzer: " + user.getUsername());
        }

        System.out.println("🔐 Führe Login durch für " + user.getUsername());
        page.fill(config.getUsernameSelector(), user.getUsername());
        page.fill(config.getPasswordSelector(), user.getDecryptedPassword());
        page.click(config.getSubmitSelector());

        // Optional: OTP hinterher
        if (user.getOtpSecret() != null && !user.getOtpSecret().isEmpty()) {
            String otp = String.format("%06d", totpService.generateCurrentOtp(user.getOtpSecret()));
            System.out.println("🔢 OTP-Code: " + otp);
            // → ggf. weitere Verarbeitung, je nach Zielseite
        }
    }
}
