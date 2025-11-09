package de.bund.zrb.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test für Preview-Tab Verhalten des LeftDrawer.
 */
class LeftDrawerTest {
    private JTabbedPane tabs;
    private JTree testTree;
    private LeftDrawer leftDrawer;

    @BeforeEach
    void setUp() {
        tabs = new JTabbedPane();
        leftDrawer = new LeftDrawer(tabs);
        try {
            Field f = LeftDrawer.class.getDeclaredField("testTree");
            f.setAccessible(true);
            testTree = (JTree) f.get(leftDrawer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void previewSelection_ShouldCreateOrReplacePreviewTab() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) testTree.getModel().getRoot();
        if (root.getChildCount() == 0) return; // nichts zu testen
        DefaultMutableTreeNode first = (DefaultMutableTreeNode) root.getChildAt(0);
        testTree.setSelectionPath(new TreePath(first.getPath()));
        assertEquals(1, tabs.getTabCount(), "Ein Preview-Tab wird erwartet");
        String title = tabs.getTitleAt(0);
        assertNotNull(title);
        assertFalse(title.startsWith("Preview:"), "Altes Präfix darf nicht mehr vorkommen");
    }
}