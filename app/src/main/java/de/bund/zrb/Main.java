package de.bund.zrb;

import de.bund.zrb.service.RecorderEventBridge;
import de.bund.zrb.ui.MainWindow;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Install event bridge before any UI is created
        RecorderEventBridge.install();

        // Start UI on EDT
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                new MainWindow().initUI();
            }
        });
    }
}
