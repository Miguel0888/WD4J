package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.TestCase;

import javax.swing.*;
import java.awt.*;

public class CaseEditorTab extends AbstractEditorTab {

    public CaseEditorTab(TestCase testCase) {
        super("Testfall bearbeiten", testCase);
        setLayout(new BorderLayout());
        add(new JLabel("Editor für Testfall: " + testCase.getName()), BorderLayout.NORTH);
    }
}