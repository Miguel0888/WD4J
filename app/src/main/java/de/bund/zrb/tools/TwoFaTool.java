package de.bund.zrb.tools;

import de.bund.zrb.service.BrowserService;

public class TwoFaTool {

    private final BrowserService browserService;

    public TwoFaTool(BrowserService browserService) {
        this.browserService = browserService;
    }

    public void performTwoFactorAuth() {
        System.out.println("2FA Tool wird ausgeführt…");
        // TODO: Implementiere hier dein 2FA-Handling
    }
}
