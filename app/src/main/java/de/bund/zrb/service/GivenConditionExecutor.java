package de.bund.zrb.service;

import com.microsoft.playwright.Page;
import de.bund.zrb.model.Precondtion;

import java.util.Map;

public class GivenConditionExecutor {

    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();

    public void apply(String username, Precondtion given) {
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

            default:
                System.err.println("⚠️ Unbekannter Given-Typ: " + type);
        }
    }

}
