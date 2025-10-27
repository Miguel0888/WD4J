package de.bund.zrb.ui.leftdrawer;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.tabs.ActionEditorTab;
import de.bund.zrb.ui.tabs.CaseEditorTab;
import de.bund.zrb.ui.tabs.SuiteEditorTab;
import de.bund.zrb.ui.tabs.UIHelper;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * Open editor tabs for suite/case/action.
 *
 * Intent:
 * - Keep left drawer navigation simple: clicking a node opens the appropriate editor tab in the main tabbed pane.
 *
 * Important:
 * - For TestAction nodes we now need to provide the list of GivenCondition objects
 *   from the parent TestCase, so the ActionEditorTab can populate the Value-dropdown
 *   with expressions from all Givens.
 *
 * Fallback:
 * - If something is malformed (no parent TestCase), we still open ActionEditorTab
 *   with an empty Given list, so the UI doesn't break.
 */
public final class EditorTabOpener {

    private EditorTabOpener() {}

    public static void openEditorTab(Component drawer, TestNode node) {
        Object ref = node.getModelRef();
        JComponent tab = null;
        String title = node.toString();

        if (ref instanceof TestAction) {
            TestAction action = (TestAction) ref;

            // Versuche den umgebenden TestCase über den Parent-Knoten zu finden
            List<GivenCondition> givensForThisAction = Collections.<GivenCondition>emptyList();

            TestNode parentNode = (TestNode) node.getParent();
            if (parentNode != null) {
                Object parentRef = parentNode.getModelRef();
                if (parentRef instanceof TestCase) {
                    TestCase tc = (TestCase) parentRef;
                    givensForThisAction = tc.getGiven(); // echte Givens des TestCase
                }
            }

            // Erzeuge ActionEditorTab mit Action + Givens
            tab = new ActionEditorTab(action, givensForThisAction);

        } else if (ref instanceof TestCase) {
            TestNode parent = (TestNode) node.getParent();
            Object suiteRef = (parent != null) ? parent.getModelRef() : null;

            if (suiteRef instanceof TestSuite) {
                tab = new CaseEditorTab((TestSuite) suiteRef, (TestCase) ref);
            } else {
                // Fallback für Fälle ohne saubere Suite-Verankerung
                tab = new CaseEditorTab(null, (TestCase) ref);
            }

        } else if (ref instanceof TestSuite) {
            tab = new SuiteEditorTab((TestSuite) ref);
        }

        if (tab != null) {
            Component parentWindow = SwingUtilities.getWindowAncestor(drawer);
            if (parentWindow instanceof JFrame) {
                JTabbedPane tabbedPane = UIHelper.findTabbedPane((JFrame) parentWindow);
                if (tabbedPane != null) {
                    tabbedPane.addTab(title, tab);
                    tabbedPane.setSelectedComponent(tab);
                }
            }
        }
    }
}
