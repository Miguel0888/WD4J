package de.bund.zrb.ui.commands.tools;

import de.bund.zrb.service.BrowserService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CaptureScreenshotCommand extends ShortcutMenuCommand {

    private final BrowserService browserService;

    public CaptureScreenshotCommand(BrowserService browserService) {
        this.browserService = browserService;
    }

    @Override
    public String getId() {
        return "tools.captureScreenshot";
    }

    @Override
    public String getLabel() {
        return "Screenshot aufnehmen";
    }

    @Override
    public void perform() {
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
