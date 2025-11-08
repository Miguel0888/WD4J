package de.bund.zrb;

import com.appland.appmap.record.Recorder;
import de.bund.zrb.service.RecorderEventBridge;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.MainWindow;

import javax.swing.SwingUtilities;

public class Main {
    public static final Recorder RECORDER = Recorder.getInstance();

    public static void main(String[] args) {
        // Install event bridge before any UI is created
        RecorderEventBridge.install();
        SettingsService.initAdapter();

        // Start UI on EDT
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                convertLegacyTestFile();

                new MainWindow().initUI();
            }
        });
    }

    @Deprecated
    private static void convertLegacyTestFile() {
        TestRegistry reg = TestRegistry.getInstance();

        if (reg.wasLoadedFromLegacy()) {
            // Nur wenn wir wirklich ein altes Array migriert haben:
            reg.save();
            System.out.println("tests.json auf neues Format migriert.");
        }
    }
}
