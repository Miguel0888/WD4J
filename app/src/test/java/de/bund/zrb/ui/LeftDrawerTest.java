package de.bund.zrb.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.lang.reflect.Field;

/**
 * Test für Preview-Tab Verhalten des LeftDrawer.
 */
class LeftDrawerTest {
    private JTabbedPane dummyTabs;
    private JTree realTree; // vom LeftDrawer erzeugt

    @BeforeEach
    void setUp() {
        dummyTabs = new JTabbedPane();
        LeftDrawer leftDrawer = new LeftDrawer(dummyTabs);

        try {
            Field f = LeftDrawer.class.getDeclaredField("testTree");
            f.setAccessible(true);
            realTree = (JTree) f.get(leftDrawer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void previewSelection_ShouldCreateOrReplacePreviewTab() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) realTree.getModel().getRoot();
        if (root.getChildCount() > 0) {
            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) root.getChildAt(0);
            realTree.setSelectionPath(new TreePath(firstChild.getPath()));
            boolean found = false;
            for (int i = 0; i < dummyTabs.getTabCount(); i++) {
                String title = dummyTabs.getTitleAt(i);
                if (title != null && title.startsWith("Preview:")) {
                    found = true;
                    break;
                }
            }
            assert found : "Preview-Tab wurde nicht angelegt";
        }
    }
}