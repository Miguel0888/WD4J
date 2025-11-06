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
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ScreenshotTool extends AbstractUserTool implements BuiltinTool {

    private final BrowserService browserService;

    public ScreenshotTool(BrowserService browserService) {
        this.browserService = browserService;
    }

    // ----------------- Builtins (unverändert aus der vorherigen Antwort) -----------------

    public Collection<ExpressionFunction> builtinFunctions() {
        List<ExpressionFunction> list = new ArrayList<ExpressionFunction>();

        list.add(new ToolExpressionFunction(
                ToolExpressionFunction.meta(
                        "Screenshot",
                        "Create a screenshot of the current page; optionally clip by CSS selector. " +
                                "Saves it in the current report and appends it to the HTML log.",
                        ToolExpressionFunction.params("selector?"),
                        Arrays.asList("Optional CSS selector to clip the screenshot to a specific element.")
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

    // ----------------- Public convenience for Commands -----------------

    /**
     * Capture (current user), persist in report, append log step, return relative image path (or null on error).
     */
    public String captureForCurrentUserAndLogReturningRel(String label, String selectorOrNull) {
        try {
            UserRegistry.User user = getCurrentUserOrFail();
            Page page = browserService.getActivePage(user.getUsername());
            if (page == null) {
                JOptionPane.showMessageDialog(null,
                        "Kein aktiver Tab für Benutzer: " + user.getUsername(),
                        "Screenshot", JOptionPane.WARNING_MESSAGE);
                return null;
            }

            byte[] png = (selectorOrNull != null)
                    ? page.locator(selectorOrNull).screenshot()
                    : page.screenshot();

            TestPlayerService tps = TestPlayerService.getInstance();
            String rel = tps.saveScreenshotFromTool(png, safeBaseName(label != null ? label : "shot"));
            tps.logScreenshotFromTool(label, rel, true, null);
            return rel;
        } catch (Exception ex) {
            try {
                TestPlayerService.getInstance().logScreenshotFromTool(
                        label, null, false,
                        (ex.getMessage() != null) ? ex.getMessage() : ex.toString()
                );
            } catch (Throwable ignore) {}
            JOptionPane.showMessageDialog(null,
                    "Screenshot fehlgeschlagen: " + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    /**
     * One-stop helper for menu/shortcut:
     * - capture for current user
     * - persist & log
     * - show a simple viewer window with the image (until echte TestPlayer-Tab Integration da ist)
     */
    public void captureAndShowInWindow(String label) {
        String rel = captureForCurrentUserAndLogReturningRel(label, null);
        if (rel == null) return;

        // Minimaler, unabhängiger Viewer (JFrame) – kann später durch "TestPlayerTab" ersetzt werden.
        JFrame f = new JFrame(label != null ? label : "Screenshot");
        f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        f.setLayout(new BorderLayout());

        // Bild als URL relativ zum Report-HTML laden ist nicht trivial -> zeige es als <img> in JEditorPane.
        // Einfacher: nutze absoluten Pfad aus dem Report-Logger? Wir haben nur 'rel'.
        // Lösung: baue die absolute URL über den Logger-Dokumentpfad.
        try {
            // Wir nutzen den TestPlayerService, um den absoluten Pfad aufzubauen.
            // relToHtml() hast du schon; hier zeigen wir das Bild über <img src=...> an.
            String html = "<html><body style='margin:0;padding:0'>" +
                    "<img src='" + rel.replace("'", "%27") + "' style='max-width:100%;'/>" +
                    "</body></html>";
            JEditorPane pane = new JEditorPane("text/html", html);
            pane.setEditable(false);
            pane.setOpaque(true);

            JScrollPane sp = new JScrollPane(pane);
            f.add(sp, BorderLayout.CENTER);
            f.setSize(900, 700);
            f.setLocationByPlatform(true);
            f.setVisible(true);
        } catch (Exception ignore) {
            // Fallback: einfache Message
            JOptionPane.showMessageDialog(null, "Screenshot gespeichert: " + rel, "Screenshot", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ----------------- Internals (wie gehabt) -----------------

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

            byte[] png = (selectorOrNull != null)
                    ? page.locator(selectorOrNull).screenshot()
                    : page.screenshot();

            TestPlayerService tps = TestPlayerService.getInstance();
            String rel = tps.saveScreenshotFromTool(png, safeBaseName(label != null ? label : "shot"));
            tps.logScreenshotFromTool(label, rel, true, null);

        } catch (Exception ex) {
            try {
                TestPlayerService.getInstance().logScreenshotFromTool(
                        label, null, false,
                        (ex.getMessage() != null) ? ex.getMessage() : ex.toString()
                );
            } catch (Throwable ignore) {}
            JOptionPane.showMessageDialog(null,
                    "Screenshot fehlgeschlagen: " + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
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
        return t.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
