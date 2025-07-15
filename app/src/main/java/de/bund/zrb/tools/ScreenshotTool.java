package de.bund.zrb.tools;

import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.UserRegistry;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ScreenshotTool extends AbstractUserTool {

    private final BrowserService browserService;

    public ScreenshotTool(BrowserService browserService) {
        this.browserService = browserService;
    }

    public void captureAndSaveForCurrentUser() {
        UserRegistry.User user = getCurrentUserOrFail(); // aus AbstractUserTool extrahieren
        captureAndSave(user);
    }


    /**
     * Captures a screenshot of the currently active global page.
     */
    public void captureAndSave() {
        captureAndSaveInternal(browserService::captureScreenshot);
    }

    /**
     * Captures a screenshot for the specified user context.
     * @param user the user whose page should be captured
     */
    public void captureAndSave(UserRegistry.User user) {
        if (user == null || user.getUsername() == null) {
            JOptionPane.showMessageDialog(null, "Ungültiger Benutzer für Screenshot.", "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        captureAndSaveInternal(() -> browserService.getActivePage(user.getUsername()).screenshot());
    }

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
}
