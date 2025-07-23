package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.TestAction;

import javax.swing.*;
import java.awt.*;

public class ActionEditorTab extends AbstractEditorTab {

    public ActionEditorTab(TestAction action) {
        super("Aktion bearbeiten", action);

        setLayout(new BorderLayout());
        add(new JLabel("Editor für Aktion: " + action.getAction()), BorderLayout.NORTH);
        // Hier kann später ein Formular mit Eingabefeldern ergänzt werden
    }
}