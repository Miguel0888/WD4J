package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.TestSuite;

import javax.swing.*;
import java.awt.*;

public class SuiteEditorTab extends AbstractEditorTab {

    public SuiteEditorTab(TestSuite suite) {
        super("Testsuite bearbeiten", suite);
        setLayout(new BorderLayout());
        add(new JLabel("Editor für Suite: " + suite.getName()), BorderLayout.NORTH);
    }
}
