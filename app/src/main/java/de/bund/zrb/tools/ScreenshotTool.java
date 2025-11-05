package de.bund.zrb.tools;

import de.bund.zrb.expressions.builtins.tooling.BuiltinTool;
import de.bund.zrb.expressions.builtins.tooling.ToolExpressionFunction;
import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionContext;
import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.UserRegistry;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ScreenshotTool extends AbstractUserTool implements BuiltinTool {

    private final BrowserService browserService;

    public ScreenshotTool(BrowserService browserService) {
        this.browserService = browserService;
    }

    /**
     * Take screenshot of the current page and save it (no clipping).
     * Return empty string to satisfy Expression runtime.
     */
    public String capture() {
        captureAndSave();
        return "";
    }

    /**
     * Take screenshot of the current page, clipped to the given CSS selector, and save it.
     * Return empty string to satisfy Expression runtime.
     */
    public String captureCurrent(String selector) {
        if (trimToNull(selector) == null) {
            return capture();
        }
        captureAndSaveInternal(new ScreenshotProvider() {
            public byte[] capture() throws IOException {
                // Use Playwright's locator.screenshot() to clip to the element.
                return browserService.getActivePage().locator(selector).screenshot();
            }
        });
        return "";
    }

    // ToDo im Code kommentiert: Sollte void sein; Expression-API verlangt aktuell String.
    public String captureAndSaveForCurrentUser() {
        UserRegistry.User user = getCurrentUserOrFail();
        captureAndSave(user);
        return "";
    }

    /**
     * Capture a screenshot of the currently active global page.
     */
    public void captureAndSave() {
        captureAndSaveInternal(new ScreenshotProvider() {
            public byte[] capture() throws IOException {
                return browserService.captureScreenshot();
            }
        });
    }

    /**
     * Capture a screenshot for the specified user context (no clipping).
     * @param user the user whose active page should be captured
     */
    public void captureAndSave(UserRegistry.User user) {
        if (user == null || user.getUsername() == null) {
            JOptionPane.showMessageDialog(null, "Ungültiger Benutzer für Screenshot.", "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        captureAndSaveInternal(new ScreenshotProvider() {
            public byte[] capture() throws IOException {
                return browserService.getActivePage(user.getUsername()).screenshot();
            }
        });
    }

    /**
     * Infrastructure: handle screenshot capture and "Save As" dialog.
     * Keep UI concerns here to respect SRP.
     */
    private void captureAndSaveInternal(ScreenshotProvider screenshotProvider) {
        try {
            byte[] screenshot = screenshotProvider.capture();

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Screenshot speichern");

            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(screenshot);
                }
                JOptionPane.showMessageDialog(null, "Screenshot gespeichert: " + file.getAbsolutePath());
            }

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Fehler beim Speichern: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    @FunctionalInterface
    private interface ScreenshotProvider {
        byte[] capture() throws IOException;
    }

    /**
     * Register builtin function:
     * Screenshot(selector?)
     * - Without selector: Capture full page of current tab.
     * - With selector:    Clip to element defined by CSS selector.
     */
    public Collection<ExpressionFunction> builtinFunctions() {
        List<ExpressionFunction> list = new ArrayList<ExpressionFunction>();

        // Screenshot(selector?)
        list.add(new ToolExpressionFunction(
                ToolExpressionFunction.meta(
                        "Screenshot",
                        "Create a screenshot of the current page; optionally clip by CSS selector.",
                        ToolExpressionFunction.params("selector?"),
                        Arrays.asList(
                                "Optional CSS selector to clip the screenshot to a specific element."
                        )
                ),
                0, 1,
                new ToolExpressionFunction.Invoker() {
                    public String invoke(List<String> args, FunctionContext ctx) throws Exception {
                        String sel = args.size() >= 1 ? trimToNull(args.get(0)) : null;
                        return sel != null ? captureCurrent(sel) : captureAndSaveForCurrentUser();
                    }
                }
        ));

        return list;
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() == 0 ? null : t;
    }
}
