package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.RootNode;
import de.bund.zrb.service.TestRegistry;

import javax.swing.*;
import java.awt.*;

/**
 * Editor-UI für den RootNode:
 *
 * Tabs:
 *  - BeforeAll (Variablen, die EINMAL am Anfang evaluiert werden)
 *  - BeforeEach (Variablen, die vor jeder Suite/Case ausgeführt werden)
 *  - Templates (Funktionszeiger, lazy im When per *name)
 *
 * Alle Änderungen werden sofort in TestRegistry gespeichert.
 */
public class RootScopeEditorTab extends JPanel {

    private final RootNode root;

    public RootScopeEditorTab(RootNode root) {
        super(new BorderLayout());
        this.root = root;

        buildUI();
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();

        // Save callback, zentral
        Runnable saveFn = new Runnable() {
            @Override
            public void run() {
                TestRegistry.getInstance().save();
            }
        };

        // BeforeAll -> root.getBeforeAllVars()
        ScopeTablePanel beforeAllPanel = new ScopeTablePanel(
                ScopeTablePanel.Mode.MODE_VARIABLES,
                root.getBeforeAllVars(),
                null,
                saveFn
        );

        // BeforeEach -> root.getBeforeEachVars()
        ScopeTablePanel beforeEachPanel = new ScopeTablePanel(
                ScopeTablePanel.Mode.MODE_VARIABLES,
                root.getBeforeEachVars(),
                null,
                saveFn
        );

        // Templates -> root.getTemplates()
        ScopeTablePanel templatesPanel = new ScopeTablePanel(
                ScopeTablePanel.Mode.MODE_TEMPLATES,
                null,
                root.getTemplates(),
                saveFn
        );

        tabs.addTab("BeforeAll", beforeAllPanel);
        tabs.addTab("BeforeEach", beforeEachPanel);
        tabs.addTab("Templates", templatesPanel);

        // Optional: kleine Info oben
        JTextArea info = new JTextArea(
                "Globaler Scope (Root):\n" +
                        "- BeforeAll: Variablen, die genau einmal vor ALLEM evaluiert werden.\n" +
                        "- BeforeEach: Variablen, die vor jeder Suite/Case (später) evaluiert werden.\n" +
                        "- Templates: Funktionszeiger (lazy via *name im When).\n" +
                        "Änderungen werden automatisch gespeichert."
        );
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setBackground(getBackground());

        add(info, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }
}
