package de.bund.zrb.ui;

import javax.swing.*;

public interface UIComponent {
    default JToolBar getToolbar() {
        return null; // Standard: Keine Toolbar
    }

    default JPanel getPanel() {
        return null; // Standard: Kein Panel
    }

    default JMenuItem getMenuItem() {
        return null; // Standard: Kein Menüeintrag
    }

    String getComponentTitle();
}
