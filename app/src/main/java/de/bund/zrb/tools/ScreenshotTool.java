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
        CapturedShot shot = captureAndPersistWithLog(label, selectorOrNull);
        return shot != null ? shot.rel : null;
    }

    private static final class CapturedShot {
        final byte[] png;
        final String rel; // may be null on error
        CapturedShot(byte[] png, String rel) { this.png = png; this.rel = rel; }
    }

    // --- NEW: capture bytes for current user; do not save/log here ---
    private byte[] captureBytesForCurrentUser(String selectorOrNull) throws Exception {
        UserRegistry.User user = getCurrentUserOrFail();
        com.microsoft.playwright.Page page = browserService.getActivePage(user.getUsername());
        if (page == null) throw new IllegalStateException("Kein aktiver Tab für Benutzer: " + user.getUsername());
        if (selectorOrNull != null) {
            return page.locator(selectorOrNull).screenshot();
        }
        return page.screenshot();
    }

    private CapturedShot captureAndPersistWithLog(String label, String selectorOrNull) {
        try {
            byte[] png = captureBytesForCurrentUser(selectorOrNull);

            // Persist + log via TestPlayerService
            de.bund.zrb.service.TestPlayerService tps = de.bund.zrb.service.TestPlayerService.getInstance();
            String rel = tps.saveScreenshotFromTool(png, safeBaseName(label != null ? label : "shot"));
            tps.logScreenshotFromTool(label, rel, true, null);

            return new CapturedShot(png, rel);

        } catch (Exception ex) {
            try {
                de.bund.zrb.service.TestPlayerService.getInstance().logScreenshotFromTool(
                        label, null, false, (ex.getMessage() != null) ? ex.getMessage() : ex.toString()
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
        CapturedShot shot = captureAndPersistWithLog(label, null);
        if (shot == null || shot.png == null || shot.png.length == 0) return;

        // Build image safely from bytes
        javax.swing.ImageIcon icon = new javax.swing.ImageIcon(shot.png);
        if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            JOptionPane.showMessageDialog(null, "Screenshot gespeichert, aber Anzeige fehlgeschlagen.", "Hinweis", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Simple, scrollable viewer window
        JFrame f = new JFrame(label != null ? label : "Screenshot");
        f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        f.setLayout(new BorderLayout());

        JLabel img = new JLabel(icon);
        img.setHorizontalAlignment(SwingConstants.CENTER);

        JScrollPane sp = new JScrollPane(img);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        sp.getHorizontalScrollBar().setUnitIncrement(24);

        // Optional header with path info
        if (shot.rel != null) {
            JPanel header = new JPanel(new BorderLayout());
            header.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            header.add(new JLabel("Gespeichert im Report unter: " + shot.rel), BorderLayout.WEST);
            f.add(header, BorderLayout.NORTH);
        }

        f.add(sp, BorderLayout.CENTER);
        f.setSize(1000, 750);
        f.setLocationByPlatform(true);
        f.setVisible(true);
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
