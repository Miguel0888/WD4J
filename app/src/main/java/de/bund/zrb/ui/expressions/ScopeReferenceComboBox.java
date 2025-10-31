package de.bund.zrb.ui.expressions;

import javax.swing.*;

public class ScopeReferenceComboBox extends JPanel {
    private final JTextField displayField;
    private final JButton dropButton;
    private final JPopupMenu popup;
    private final JList<String> list; // einfache Liste mit Namen und *Namen

    private String chosenName; // z. B. "username" oder "*otpCode"

    public ScopeReferenceComboBox() { ... }

    public void setScopeData(GivenLookupService.ScopeData data) {
        // baue eine DefaultListModel<String>
        // 1. erst alle normalen Variablen (keys von data.variables)
        // 2. dann alle Templates, aber mit "*" vorne dran
        // speicher das im JList
    }

    public String getChosenName() {
        return chosenName;
    }
}
