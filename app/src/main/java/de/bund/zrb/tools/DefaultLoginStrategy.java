package de.bund.zrb.tools;

import com.microsoft.playwright.Page;
import de.bund.zrb.service.TotpService;
import de.bund.zrb.service.UserRegistry;

public class DefaultLoginStrategy implements LoginStrategy {

    private final TotpService totpService;

    public DefaultLoginStrategy(TotpService totpService) {
        this.totpService = totpService;
    }

    @Override
    public void performLogin(Page page, UserRegistry.User user) {
        page.waitForSelector("input[name='username']");
        page.fill("input[name='username']", user.getUsername());
        page.fill("input[name='password']", user.getDecryptedPassword());

        if (user.getOtpSecret() != null && !user.getOtpSecret().isEmpty()) {
            int otp = totpService.generateCurrentOtp(user.getOtpSecret());
            page.fill("input[name='otp']", String.format("%06d", otp));
        }

        page.click("button[type='submit']");
    }
}
