package de.bund.zrb.tools;

import de.bund.zrb.service.BrowserService;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ScreenshotTool {

    private final BrowserService browserService;

    public ScreenshotTool(BrowserService browserService) {
        this.browserService = browserService;
    }

    public void captureAndSave() {
        try {
            byte[] screenshot = browserService.captureScreenshot();
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
}
