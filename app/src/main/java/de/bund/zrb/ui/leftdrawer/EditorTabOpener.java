package de.bund.zrb.ui.leftdrawer;

import de.bund.zrb.model.RootNode;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.giveneditor.RootScopeEditorTab;

import javax.swing.*;
import java.awt.*;

public class EditorTabOpener {

    /**
     * Öffnet/aktiviert den passenden Editor-Tab für die angeklickte Node.
     *
     * @param parent irgendeine Component aus dem UI (für JOptionPane etc.)
     * @param editorTabs das zentrale TabbedPane aus MainWindow (deine Mitte)
     * @param node der geklickte TestNode aus dem linken Baum
     */
    public static void openEditorTab(Component parent,
                                     JTabbedPane editorTabs,
                                     TestNode node) {

        if (node == null || editorTabs == null) return;
        Object ref = node.getModelRef();
        if (ref == null) return;

        // 1. RootNode -> RootScopeEditorTab
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
            editorTabs.setSelectedComponent(panel);
            return;
        }

        // 2. TestSuite -> (SuiteScopeEditorTab kommt später)
        if (ref instanceof TestSuite) {
            TestSuite suite = (TestSuite) ref;
            String tabTitle = "Suite: " + safe(suite.getName());

            int idx = findTabByTitle(editorTabs, tabTitle);
            if (idx >= 0) {
                editorTabs.setSelectedIndex(idx);
                return;
            }

            // Platzhalter bis wir SuiteScopeEditorTab bauen
            JPanel placeholder = new JPanel(new BorderLayout());
            placeholder.add(new JLabel("Suite-Editor TODO für: " + safe(suite.getName())), BorderLayout.CENTER);

            editorTabs.addTab(tabTitle, placeholder);
            editorTabs.setSelectedComponent(placeholder);
            return;
        }

        // 3. TestCase -> (CaseScopeEditorTab kommt später)
        if (ref instanceof TestCase) {
            TestCase tc = (TestCase) ref;
            String tabTitle = "Case: " + safe(tc.getName());

            int idx = findTabByTitle(editorTabs, tabTitle);
            if (idx >= 0) {
                editorTabs.setSelectedIndex(idx);
                return;
            }

            JPanel placeholder = new JPanel(new BorderLayout());
            placeholder.add(new JLabel("Case-Editor TODO für: " + safe(tc.getName())), BorderLayout.CENTER);

            editorTabs.addTab(tabTitle, placeholder);
            editorTabs.setSelectedComponent(placeholder);
            return;
        }

        // 4. TestAction -> ActionEditorTab (später ersetzen durch deine echte ActionEditorTab-Logik)
        if (ref instanceof TestAction) {
            TestAction action = (TestAction) ref;
            String tabTitle = "Action: " + safe(action.getAction());

            int idx = findTabByTitle(editorTabs, tabTitle);
            if (idx >= 0) {
                editorTabs.setSelectedIndex(idx);
                return;
            }

            JPanel placeholder = new JPanel(new BorderLayout());
            placeholder.add(new JLabel("Action Editor TODO für: " + safe(action.getAction())), BorderLayout.CENTER);

            editorTabs.addTab(tabTitle, placeholder);
            editorTabs.setSelectedComponent(placeholder);
            return;
        }

        JOptionPane.showMessageDialog(parent,
                "Kein Editor für Typ: " + ref.getClass().getSimpleName());
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
