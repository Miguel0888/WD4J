package de.bund.zrb.ui.leftdrawer;

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

/**
 * Open editor tabs for suite/case/action. Keep behavior identical to original.
 */
public final class EditorTabOpener {

    private EditorTabOpener() {}

    public static void openEditorTab(Component drawer, TestNode node) {
        Object ref = node.getModelRef();
        JComponent tab = null;
        String title = node.toString();

        if (ref instanceof TestAction) {
            tab = new ActionEditorTab((TestAction) ref);
        } else if (ref instanceof TestCase) {
            TestNode parent = (TestNode) node.getParent();
            Object suiteRef = parent.getModelRef();
            if (suiteRef instanceof TestSuite) {
                tab = new CaseEditorTab((TestSuite) suiteRef, (TestCase) ref);
            } else {
                tab = new CaseEditorTab(null, (TestCase) ref); // fallback
            }
        } else if (ref instanceof TestSuite) {
            tab = new SuiteEditorTab((TestSuite) ref);
        }

        if (tab != null) {
            Component parent = SwingUtilities.getWindowAncestor(drawer);
            if (parent instanceof JFrame) {
                JTabbedPane tabbedPane = UIHelper.findTabbedPane((JFrame) parent);
                if (tabbedPane != null) {
                    tabbedPane.addTab(title, tab);
                    tabbedPane.setSelectedComponent(tab);
                }
            }
        }
    }
}
