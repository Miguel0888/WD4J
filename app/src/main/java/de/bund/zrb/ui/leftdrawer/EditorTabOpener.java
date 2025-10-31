package de.bund.zrb.ui.leftdrawer;

import de.bund.zrb.model.RootNode;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.giveneditor.CaseScopeEditorTab;
import de.bund.zrb.ui.giveneditor.RootScopeEditorTab;
import de.bund.zrb.ui.giveneditor.SuiteScopeEditorTab;
import de.bund.zrb.ui.tabs.ClosableTabHeader;

import javax.swing.*;
import java.awt.*;

public class EditorTabOpener {

    /**
     * Öffnet/aktiviert den passenden Editor-Tab für die angeklickte Node.
     *
     * @param parent      irgendeine Component aus dem UI (für JOptionPane etc.)
     * @param editorTabs  das zentrale TabbedPane aus MainWindow (deine Mitte)
     * @param node        der geklickte TestNode aus dem linken Baum
     */
    public static void openEditorTab(Component parent,
                                     JTabbedPane editorTabs,
                                     TestNode node) {

        if (node == null || editorTabs == null) return;
        Object ref = node.getModelRef();
        if (ref == null) return;

        // ---- RootNode Tab ----
        if (ref instanceof RootNode) {
            RootNode rn = (RootNode) ref;
            String tabTitle = "Root Scope";

            int idx = findTabByTitle(editorTabs, tabTitle);
            if (idx >= 0) {
                editorTabs.setSelectedIndex(idx);
                return;
            }

            RootScopeEditorTab panel = new RootScopeEditorTab(rn);

            // Tab hinzufügen
            editorTabs.addTab(tabTitle, panel);
            int newIdx = editorTabs.indexOfComponent(panel);

            // Header mit rotem X dranbauen
            editorTabs.setTabComponentAt(newIdx,
                    new ClosableTabHeader(editorTabs, panel, tabTitle));

            // Tab aktivieren
            editorTabs.setSelectedIndex(newIdx);
            return;
        }

        // ---- TestSuite Tab ----
        if (ref instanceof TestSuite) if (ref instanceof TestSuite) {
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

            editorTabs.setTabComponentAt(newIdx,
                    new ClosableTabHeader(editorTabs, panel, tabTitle));

            editorTabs.setSelectedIndex(newIdx);
            return;
        }

        // ---- TestCase Tab ----
        if (ref instanceof TestCase) if (ref instanceof TestCase) {
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

            editorTabs.setTabComponentAt(newIdx,
                    new ClosableTabHeader(editorTabs, panel, tabTitle));

            editorTabs.setSelectedIndex(newIdx);
            return;
        }

        // ---- TestAction Tab ----
        if (ref instanceof TestAction) {
            TestAction action = (TestAction) ref;
            String tabTitle = "Action: " + safe(action.getAction());

            int idx = findTabByTitle(editorTabs, tabTitle);
            if (idx >= 0) {
                editorTabs.setSelectedIndex(idx);
                return;
            }

            JPanel placeholder = new JPanel(new BorderLayout());
            placeholder.add(new JLabel("Action Editor TODO für: " + safe(action.getAction())),
                    BorderLayout.CENTER);

            editorTabs.addTab(tabTitle, placeholder);
            int newIdx = editorTabs.indexOfComponent(placeholder);

            editorTabs.setTabComponentAt(newIdx,
                    new ClosableTabHeader(editorTabs, placeholder, tabTitle));

            editorTabs.setSelectedIndex(newIdx);
            return;
        }

        // Fallback
        JOptionPane.showMessageDialog(
                parent,
                "Kein Editor für Typ: " + ref.getClass().getSimpleName()
        );
    }

    private static int findTabByTitle(JTabbedPane tabs, String wanted) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (wanted.equals(tabs.getTitleAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "(unnamed)" : s.trim();
    }
}
