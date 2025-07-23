package de.bund.zrb;

import de.bund.zrb.ui.MainWindow;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().initUI());
    }
}
