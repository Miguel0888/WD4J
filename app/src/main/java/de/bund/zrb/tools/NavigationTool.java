// src/main/java/de/bund/zrb/tools/NavigationTool.java
package de.bund.zrb.tools;

import com.microsoft.playwright.Page;
import de.bund.zrb.expressions.builtins.tooling.BuiltinTool;
import de.bund.zrb.expressions.builtins.tooling.ToolExpressionFunction;
import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionContext;
import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.UserRegistry;

import java.util.*;

/**
 * Navigationsfunktionen für Playwright-Seiten je Benutzer.
 * Bietet zusätzlich Built-in-Funktionen für Expressions an.
 */
public class NavigationTool extends AbstractUserTool implements BuiltinTool {

    private final BrowserService browserService;

    public NavigationTool(BrowserService browserService) {
        this.browserService = browserService;
    }

    /**
     * Navigiert die Seite des aktuellen Benutzers zur konfigurierten Startseite.
     */
    public void navigateToStartPage() {
        UserRegistry.User currentUser = getCurrentUserOrFail();
        navigateToStartPage(currentUser);
    }

    /**
     * Navigiert die Seite des angegebenen Benutzers zur konfigurierten Startseite.
     */
    public void navigateToStartPage(UserRegistry.User user) {
        if (user == null) {
            throw new IllegalArgumentException("Benutzer darf nicht null sein.");
        }

        String startPage = user.getStartPage();
        if (startPage == null || startPage.trim().isEmpty()) {
            throw new IllegalStateException("Startseite für Benutzer " + user.getUsername() + " ist nicht gesetzt.");
        }

        Page page = browserService.getActivePage(user.getUsername());
        if (page == null) {
            throw new IllegalStateException("Keine aktive Seite für Benutzer " + user.getUsername() + " vorhanden.");
        }

        System.out.println("🌐 Navigiere Benutzer '" + user.getUsername() + "' zu: " + startPage);
        page.navigate(startPage);
    }

    /**
     * Navigiere den aktuellen Benutzer zu einer URL.
     * @return tatsächliche Ziel-URL nach Navigation (falls Redirects)
     */
    public String navigate(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL darf nicht leer sein.");
        }
        UserRegistry.User user = getCurrentUserOrFail();
        return navigate(user, url);
    }

    /**
     * Navigiere den angegebenen Benutzer zu einer URL.
     * @return tatsächliche Ziel-URL nach Navigation (falls Redirects)
     */
    public String navigate(UserRegistry.User user, String url) {
        if (user == null) {
            throw new IllegalArgumentException("Benutzer darf nicht null sein.");
        }
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL darf nicht leer sein.");
        }

        Page page = browserService.getActivePage(user.getUsername());
        if (page == null) {
            throw new IllegalStateException("Keine aktive Seite für Benutzer " + user.getUsername() + " vorhanden.");
        }

        System.out.println("🌐 Navigiere Benutzer '" + user.getUsername() + "' zu: " + url);
        page.navigate(url);
        // Playwright liefert die aktuelle URL über page.url()
        return page.url();
    }

    // ===== BuiltinTool: Expression-Funktionen anbieten =====

    public Collection<ExpressionFunction> builtinFunctions() {
        List<ExpressionFunction> list = new ArrayList<ExpressionFunction>();

        // 1) Navigate(url) -> String
        list.add(new ToolExpressionFunction(
                ToolExpressionFunction.meta(
                        "Navigate",
                        "Navigate current user to the given URL and return the final URL.",
                        ToolExpressionFunction.params("url"),
                        Arrays.asList("Target URL (absolute or relative).")
                ),
                1, 1,
                new ToolExpressionFunction.Invoker() {
                    public String invoke(List<String> args, FunctionContext ctx) throws Exception {
                        String url = args.get(0);
                        return navigate(url);
                    }
                }
        ));

        // 2) NavigateAs(userName; url) -> String
        list.add(new ToolExpressionFunction(
                ToolExpressionFunction.meta(
                        "NavigateAs",
                        "Navigate a specific user to the given URL and return the final URL.",
                        ToolExpressionFunction.params("userName", "url"),
                        Arrays.asList("Registered user name.", "Target URL (absolute or relative).")
                ),
                2, 2,
                new ToolExpressionFunction.Invoker() {
                    public String invoke(List<String> args, FunctionContext ctx) throws Exception {
                        String userName = args.get(0);
                        String url = args.get(1);
                        UserRegistry.User user = resolveUserByName(userName);
                        return navigate(user, url);
                    }
                }
        ));

        return list;
    }

    // ---- helper ----

    private UserRegistry.User resolveUserByName(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("userName darf nicht leer sein.");
        }
        UserRegistry.User u = UserRegistry.getInstance().getUser(userName);
        if (u == null) {
            throw new IllegalStateException("Unbekannter Benutzer: " + userName);
        }
        return u;
    }
}
