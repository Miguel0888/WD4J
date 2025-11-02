package de.bund.zrb.ui.leftdrawer;

import de.bund.zrb.model.RootNode;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.giveneditor.CaseScopeEditorTab;
import de.bund.zrb.ui.giveneditor.RootScopeEditorTab;
import de.bund.zrb.ui.giveneditor.SuiteScopeEditorTab;
import de.bund.zrb.ui.tabs.ActionEditorTab;
import de.bund.zrb.ui.tabs.ClosableTabHeader;

import javax.swing.*;
import java.awt.*;

/**
 * Öffnet Editor-Tabs in der zentralen Mitte (editorTabs).
 *
 * - Root / Suite / Case Tabs werden per Titel reused.
 * - Action Tabs werden NIE reused (immer neuer Tab mit #Counter).
 *
 * Wichtiger Hinweis:
 * Wir sammeln hier KEINE "Givens"/Scopes mehr für Actions.
 * Der ActionEditorTab macht das jetzt selbst über GivenLookupService.
 * D.h. der TabOpener muss nur noch die richtige Tab-Instanz öffnen.
 */
public class EditorTabOpener {

    private static int actionCounter = 1;

    public static void openEditorTab(Component parent,
                                     JTabbedPane editorTabs,
                                     TestNode node) {

        if (node == null || editorTabs == null) return;
        Object ref = node.getModelRef();
        if (ref == null) return;

        // --------------------------------------------------------------------
        // RootNode-Tab (wird wiederverwendet)
        // --------------------------------------------------------------------
        if (ref instanceof RootNode) {
            RootNode rn = (RootNode) ref;
            String tabTitle = "Root Scope";

            int idx = findTabByTitle(editorTabs, tabTitle);
            if (idx >= 0) {
                editorTabs.setSelectedIndex(idx);
                return;
            }

            RootScopeEditorTab panel = new RootScopeEditorTab(rn);

            editorTabs.addTab(tabTitle, panel);
            int newIdx = editorTabs.indexOfComponent(panel);
            editorTabs.setTabComponentAt(
                    newIdx,
                    new ClosableTabHeader(editorTabs, panel, tabTitle)
            );
            editorTabs.setSelectedIndex(newIdx);
            return;
        }

        // --------------------------------------------------------------------
        // Suite-Tab (wird wiederverwendet)
        // --------------------------------------------------------------------
        if (ref instanceof TestSuite) {
            TestSuite suite = (TestSuite) ref;
            String tabTitle = "Suite: " + safe(suite.getName());

            int idx = findTabByTitle(editorTabs, tabTitle);
            if (idx >= 0) {
                editorTabs.setSelectedIndex(idx);
                return;
            }

            SuiteScopeEditorTab panel = new SuiteScopeEditorTab(suite);

            editorTabs.addTab(tabTitle, panel);
            int newIdx = editorTabs.indexOfComponent(panel);
            editorTabs.setTabComponentAt(
                    newIdx,
                    new ClosableTabHeader(editorTabs, panel, tabTitle)
            );
            editorTabs.setSelectedIndex(newIdx);
            return;
        }

        // --------------------------------------------------------------------
        // Case-Tab (wird wiederverwendet)
        // --------------------------------------------------------------------
        if (ref instanceof TestCase) {
            TestCase testCase = (TestCase) ref;
            String tabTitle = "Case: " + safe(testCase.getName());

            int idx = findTabByTitle(editorTabs, tabTitle);
            if (idx >= 0) {
                editorTabs.setSelectedIndex(idx);
                return;
            }

            CaseScopeEditorTab panel = new CaseScopeEditorTab(testCase);

            editorTabs.addTab(tabTitle, panel);
            int newIdx = editorTabs.indexOfComponent(panel);
            editorTabs.setTabComponentAt(
                    newIdx,
                    new ClosableTabHeader(editorTabs, panel, tabTitle)
            );
            editorTabs.setSelectedIndex(newIdx);
            return;
        }

        // --------------------------------------------------------------------
        // Action-Tab (wird NICHT wiederverwendet – jede Action bekommt neuen Tab)
        // --------------------------------------------------------------------
        if (ref instanceof TestAction) {
            TestAction action = (TestAction) ref;

            ActionEditorTab panel = new ActionEditorTab(action);

            String tabTitle = "Action: " + safe(action.getAction()) + " (#" + (actionCounter++) + ")";

            editorTabs.addTab(tabTitle, panel);
            int newIdx = editorTabs.indexOfComponent(panel);
            editorTabs.setTabComponentAt(
                    newIdx,
                    new ClosableTabHeader(editorTabs, panel, tabTitle)
            );
            editorTabs.setSelectedIndex(newIdx);
            return;
        }

        // --------------------------------------------------------------------
        // Fallback
        // --------------------------------------------------------------------
        JOptionPane.showMessageDialog(
                parent,
                "Kein Editor für Typ: " + ref.getClass().getSimpleName()
        );
    }

    /**
     * Suche nach einem Tab anhand seines Titels (wichtig für Root/Suite/Case-Reuse).
     */
    private static int findTabByTitle(JTabbedPane tabs, String wanted) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (wanted.equals(tabs.getTitleAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Null/Leer-Absicherung für Tab-Titel.
     */
    private static String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "(unnamed)" : s.trim();
    }
}
