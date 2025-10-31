package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.RootNode;
import de.bund.zrb.service.TestRegistry;

import javax.swing.*;
import java.awt.*;

/**
 * Editor-UI für den globalen Root-Scope.
 *
 * Tabs:
 *  - BeforeAll   : Variablen, die EINMAL ganz am Anfang evaluiert werden.
 *                  -> landen in root.getBeforeAll(), tauchen NICHT im When-Dropdown auf.
 *
 *  - BeforeEach  : Variablen, die vor jedem Case (bzw. jeder Suite/jedem Case)
 *                  evaluiert werden.
 *                  -> landen in root.getBeforeEach(), tauchen im When-Dropdown als normale Namen auf.
 *
 *  - Templates   : Funktionszeiger (lazy ausgewertet in WHEN-Schritten per *name).
 *                  -> landen in root.getTemplates(), tauchen im When-Dropdown mit führendem * auf.
 *
 * Alle Änderungen werden in-memory am RootNode vorgenommen und bei "Speichern"
 * über TestRegistry.persistiert.
 */
public class RootScopeEditorTab extends JPanel {

    private final RootNode root;

    public RootScopeEditorTab(RootNode root) {
        super(new BorderLayout());
        this.root = root;
        buildUI();
    }

    private void buildUI() {
        // gemeinsamer Save-Callback
        final Runnable saveFn = new Runnable() {
            @Override
            public void run() {
                // einfach das komplette Modell persistieren
                TestRegistry.getInstance().save();
            }
        };

        // Panels für die drei Scopes des Root
        ScopeTablePanel beforeAllPanel = new ScopeTablePanel(
                ScopeTablePanel.Mode.MODE_VARIABLES,
                root.getBeforeAll(),   // Variablen, einmalig
                null,
                saveFn
        );

        ScopeTablePanel beforeEachPanel = new ScopeTablePanel(
                ScopeTablePanel.Mode.MODE_VARIABLES,
                root.getBeforeEach(),  // Variablen vor jedem Case
                null,
                saveFn
        );

        ScopeTablePanel templatesPanel = new ScopeTablePanel(
                ScopeTablePanel.Mode.MODE_TEMPLATES,
                null,
                root.getTemplates(),   // Funktionszeiger (lazy)
                saveFn
        );

        // TabbedPane für die drei Bereiche
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("BeforeAll", beforeAllPanel);
        tabs.addTab("BeforeEach", beforeEachPanel);
        tabs.addTab("Templates", templatesPanel);

        // Hinweistext oben
        JTextArea info = new JTextArea(
                "Globaler Scope (Root):\n" +
                        "- BeforeAll: Variablen, die genau einmal vor dem gesamten Lauf evaluiert werden. " +
                        "(Nicht im When-Dropdown.)\n" +
                        "- BeforeEach: Variablen, die vor jedem TestCase evaluiert werden. " +
                        "(Diese Namen tauchen im When-Dropdown normal auf.)\n" +
                        "- Templates: Funktionszeiger (lazy ausgewertet in WHEN), " +
                        "werden im When-Dropdown mit *prefix angezeigt.\n" +
                        "\n" +
                        "In jeder Tabelle kannst du Einträge hinzufügen ( + ), " +
                        "markierte Einträge entfernen ( - ) und einzelne Zeilen speichern.\n" +
                        "Der Speichern-Knopf ruft intern TestRegistry.save() auf."
        );
        info.setEditable(false);
        info.setWrapStyleWord(true);
        info.setLineWrap(true);
        info.setBackground(getBackground());
        info.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        setLayout(new BorderLayout());
        add(info, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }
}
