package de.bund.zrb.tools;

import com.microsoft.playwright.Page;
import de.bund.zrb.config.LoginConfig;
import de.bund.zrb.expressions.builtins.tooling.BuiltinTool;
import de.bund.zrb.expressions.builtins.tooling.ToolExpressionFunction;
import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionContext;
import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.TotpService;
import de.bund.zrb.service.UserRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class LoginTool extends AbstractUserTool implements BuiltinTool {

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

    public void login(UserRegistry.User user){
        login(user, null);
    }

    public void login(UserRegistry.User user, Page page) {
        String loginUrl = user.getLoginConfig().getLoginPage();
        if (loginUrl == null || loginUrl.trim().isEmpty()) {
            throw new IllegalStateException("Login-Seite nicht definiert für Benutzer: " + user.getUsername());
        }

        if( page == null)
        { // if no page is determined use the current selected one
            page = browserService.getActivePage(user.getUsername());
            page.navigate(loginUrl);
        }

        LoginConfig config = user.getLoginConfig();

        if (config.getUsernameSelector() == null ||
                config.getPasswordSelector() == null ||
                config.getSubmitSelector() == null) {
            throw new IllegalStateException("Login-Konfiguration unvollständig für Benutzer: " + user.getUsername());
        }

        System.out.println("🔐 Führe Login durch für " + user.getUsername());

        page.locator(config.getUsernameSelector()).waitFor();
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

    /**
     * Führt einen Passwort-Änderungsflow aus: Seite aufrufen (falls konfiguriert), Felder füllen und absenden.
     * Aktuell werden neues Passwort und Wiederholung mit dem gespeicherten Benutzerpasswort befüllt.
     */
    public void changePasswordForCurrentUser() {
        UserRegistry.User user = getCurrentUserOrFail();
        changePassword(user, null);
    }

    public void changePassword(UserRegistry.User user, Page page) {
        LoginConfig cfg = user.getLoginConfig();
        if (cfg == null) {
            throw new IllegalStateException("LoginConfig fehlt für Benutzer: " + user.getUsername());
        }

        // Seite öffnen, falls vorhanden
        String pwChangeUrl = cfg.getPasswordChangePage();
        if (page == null) {
            page = browserService.getActivePage(user.getUsername());
            if (pwChangeUrl != null && !pwChangeUrl.trim().isEmpty()) {
                page.navigate(pwChangeUrl);
            }
        }

        String curSel   = cfg.getCurrentPasswordSelector();
        String newSel   = cfg.getNewPasswordSelector();
        String repSel   = cfg.getRepeatPasswordSelector();
        String submitSel= cfg.getChangeSubmitSelector();

        if (newSel == null || repSel == null || submitSel == null) {
            throw new IllegalStateException("Passwort-Änderungs-Konfiguration unvollständig (neues/Repeat/Submit) für Benutzer: " + user.getUsername());
        }

        System.out.println("🔏 Passwort-Änderung für " + user.getUsername());

        // Optional: aktuelles Passwort füllen, wenn Feld existiert
        if (curSel != null && !curSel.trim().isEmpty()) {
            page.locator(curSel).waitFor();
            page.fill(curSel, user.getDecryptedPassword());
        }

        // Neues Passwort + Wiederholung befüllen (hier gleich dem gespeicherten Passwort)
        page.locator(newSel).waitFor();
        page.fill(newSel, user.getDecryptedPassword());
        page.locator(repSel).waitFor();
        page.fill(repSel, user.getDecryptedPassword());

        page.click(submitSel);
    }

    public String login(String user, String pass) {
        login(new UserRegistry.User(user, pass));
        return "login";
    }
    public String login(String user, String pass, String pageId) {
        login(new UserRegistry.User(user, pass), browserService.pageForBrowsingContextId(pageId));
        return "login";
    }

    public Collection<ExpressionFunction> builtinFunctions() {
        List<ExpressionFunction> list = new ArrayList<ExpressionFunction>();

        list.add(new ToolExpressionFunction(
                ToolExpressionFunction.meta(
                        "Login",
                        "Perform a login using username/password; optionally add a TOTP secret.",
                        ToolExpressionFunction.params("username", "password", "totpSecret?"),
                        Arrays.asList(
                                "Account user name.",
                                "Account password, encrypted.",
                                "Optional Page by its ID")
                ),
                2, 3,
                new ToolExpressionFunction.Invoker() {
                    public String invoke(List<String> args, FunctionContext ctx) throws Exception {
                        String u = args.get(0);
                        String p = args.get(1);
                        String c = args.size() >= 3 ? args.get(2) : null;
                        return c != null && c.length() > 0 ? login(u, p, c) : login(u, p);
                    }
                }
        ));

        return list;
    }
}
