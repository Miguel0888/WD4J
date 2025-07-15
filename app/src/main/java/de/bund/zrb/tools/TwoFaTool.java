package de.bund.zrb.tools;

import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.TotpService;
import de.bund.zrb.service.UserRegistry;

public class TwoFaTool {

    private final BrowserService browserService;
    private final TotpService totpService;

    public TwoFaTool(BrowserService browserService, TotpService totpService) {
        this.browserService = browserService;
        this.totpService = totpService;
    }

    /**
     * Generates a new OTP secret for the given user.
     * The caller is responsible for storing it in the UserRegistry.
     *
     * @param user the user to generate a secret for
     * @return a new TOTP secret key (Base32 encoded)
     */
    public String generateSecretFor(UserRegistry.User user) {
        String newSecret = totpService.generateSecretKey();
        System.out.println("Generated new OTP secret for user: " + user.getUsername());
        return newSecret;
    }
}
