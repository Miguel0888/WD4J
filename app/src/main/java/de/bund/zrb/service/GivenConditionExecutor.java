package de.bund.zrb.service;

import com.microsoft.playwright.Page;
import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.GivenRegistry;
import de.bund.zrb.model.GivenTypeDefinition;

import java.util.Map;

public class GivenConditionExecutor {

    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();

    public void apply(String username, GivenCondition given) {
        Page page = browserService.getActivePage(username);
        if (page == null) {
            System.err.println("⚠️ Kein aktives Browserfenster für Benutzer: " + username);
            return;
        }

        String type = given.getType();
        Map<String, Object> params = given.getParameterMap();

        switch (type) {
            case "url-is":
                String expectedUrl = (String) params.get("expectedUrl");
                if (expectedUrl != null && !expectedUrl.isEmpty()) {
                    page.navigate(expectedUrl);
                }
                break;

            case "element-exists":
                String selector = (String) params.get("selector");
                if (selector != null && !selector.isEmpty()) {
                    page.waitForSelector(selector);
                }
                break;

            case "cookie-present":
                // TODO: ggf. Cookie setzen oder prüfen
                break;

            case "localstorage-key":
                // TODO: ggf. localStorage Key setzen oder prüfen
                break;

            case "js-eval":
                String script = (String) params.get("script");
                if (script != null && !script.isEmpty()) {
                    page.evaluate(script);
                }
                break;

            case "logged-in":
                String loginUser = (String) params.get("username");
                if (loginUser != null) {
                    loginAs(page, loginUser);
                }
                break;

            default:
                System.err.println("⚠️ Unbekannter Given-Typ: " + type);
        }
    }

    private void loginAs(Page page, String username) {
        UserRegistry.User user = UserRegistry.getInstance().getUser(username);
        if (user == null) {
            throw new RuntimeException("Unbekannter Benutzer: " + username);
        }

        page.navigate(user.getLoginPage());

        page.locator(user.getLoginConfig().getUsernameSelector()).fill(user.getUsername());
        page.locator(user.getLoginConfig().getPasswordSelector()).fill(user.getDecryptedPassword());
        page.locator(user.getLoginConfig().getSubmitSelector()).click();

        // optional warten bis Startseite geladen
        if (user.getStartPage() != null) {
            page.waitForURL(user.getStartPage());
        }
    }
}
