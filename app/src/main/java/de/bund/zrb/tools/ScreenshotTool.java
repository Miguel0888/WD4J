package de.bund.zrb.tools;

import de.bund.zrb.expressions.builtins.tooling.BuiltinTool;
import de.bund.zrb.expressions.builtins.tooling.ToolExpressionFunction;
import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionContext;
import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.TestPlayerService;
import de.bund.zrb.service.UserRegistry;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Built-in tool that captures screenshots and writes them into the current run's report,
 * then appends a log entry with the image.
 *
 * Functions exposed:
 *   Screenshot()                 -> full page of current user's active tab
 *   Screenshot(selector)         -> clip to CSS selector (current user's tab)
 *   ScreenshotFor(user)          -> full page of given user's active tab
 *   ScreenshotFor(user, selector)-> clip to CSS selector for given user's tab
 *
 * All functions return an empty string "" to keep the Expression runtime happy.
 */
public class ScreenshotTool extends AbstractUserTool implements BuiltinTool {

    private final BrowserService browserService;

    public ScreenshotTool(BrowserService browserService) {
        this.browserService = browserService;
    }

    // ----------------- Builtin functions registration -----------------

    public Collection<ExpressionFunction> builtinFunctions() {
        List<ExpressionFunction> list = new ArrayList<ExpressionFunction>();

        // Screenshot(selector?)
        list.add(new ToolExpressionFunction(
                ToolExpressionFunction.meta(
                        "Screenshot",
                        "Create a screenshot of the current page; optionally clip by CSS selector. " +
                                "Saves it in the current report and appends it to the HTML log.",
                        ToolExpressionFunction.params("selector?"),
                        Arrays.asList(
                                "Optional CSS selector to clip the screenshot to a specific element."
                        )
                ),
                0, 1,
                new ToolExpressionFunction.Invoker() {
                    public String invoke(List<String> args, FunctionContext ctx) throws Exception {
                        String selector = args.size() >= 1 ? trimToNull(args.get(0)) : null;
                        captureForCurrentUserAndLog("Screenshot", selector);
                        return "";
                    }
                }
        ));

        // ScreenshotFor(user[, selector])
        list.add(new ToolExpressionFunction(
                ToolExpressionFunction.meta(
                        "ScreenshotFor",
                        "Create a screenshot for the given user; optionally clip by CSS selector. " +
                                "Saves it in the current report and appends it to the HTML log.",
                        ToolExpressionFunction.params("username", "selector?"),
                        Arrays.asList(
                                "Logical username to resolve the user's tab.",
                                "Optional CSS selector to clip the screenshot."
                        )
                ),
                1, 2,
                new ToolExpressionFunction.Invoker() {
                    public String invoke(List<String> args, FunctionContext ctx) throws Exception {
                        String user = trimToNull(args.get(0));
                        String selector = (args.size() >= 2) ? trimToNull(args.get(1)) : null;
                        if (user == null) throw new IllegalArgumentException("username must not be empty");
                        captureForUserAndLog(user, "Screenshot(" + user + ")", selector);
                        return "";
                    }
                }
        ));

        return list;
    }

    // ----------------- Implementation -----------------

    private void captureForCurrentUserAndLog(String label, String selectorOrNull) {
        UserRegistry.User user = getCurrentUserOrFail();
        captureForUserAndLog(user.getUsername(), label, selectorOrNull);
    }

    private void captureForUserAndLog(String username, String label, String selectorOrNull) {
        try {
            Page page = browserService.getActivePage(username);
            if (page == null) {
                JOptionPane.showMessageDialog(null,
                        "Kein aktiver Tab für Benutzer: " + username,
                        "Screenshot", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Capture
            byte[] png;
            if (selectorOrNull != null) {
                Locator loc = page.locator(selectorOrNull);
                png = loc.screenshot();
            } else {
                png = page.screenshot();
            }

            // Persist in report
            TestPlayerService tps = TestPlayerService.getInstance();
            String baseName = safeBaseName(label != null ? label : "shot");
            String rel = tps.saveScreenshotFromTool(png, baseName);

            // Append log entry
            tps.logScreenshotFromTool(label, rel, true, null);

        } catch (Exception ex) {
            // Log failure (without image)
            try {
                TestPlayerService.getInstance().logScreenshotFromTool(
                        label, null, false,
                        (ex.getMessage() != null) ? ex.getMessage() : ex.toString()
                );
            } catch (Throwable ignore) {
                // keep UI feedback as last resort
                JOptionPane.showMessageDialog(null,
                        "Screenshot fehlgeschlagen: " + ex.getMessage(),
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() == 0 ? null : t;
    }

    private String safeBaseName(String s) {
        String t = (s == null) ? "shot" : s.trim();
        if (t.length() == 0) t = "shot";
        // keep consistent with your saveScreenshotBytes sanitization
        return t.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
