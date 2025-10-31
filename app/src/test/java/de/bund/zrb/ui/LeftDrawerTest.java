package de.bund.zrb.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

class LeftDrawerTest {
    private LeftDrawer leftDrawer;
    private JTree mockTree;
    private DefaultTreeModel mockModel;
    private DefaultMutableTreeNode parentNode;
    private DefaultMutableTreeNode childNode;

    @BeforeEach
    void setUp() {
        leftDrawer = new LeftDrawer(null);

        mockTree = mock(JTree.class);
        mockModel = mock(DefaultTreeModel.class);
        parentNode = new DefaultMutableTreeNode("parent");
        childNode = new DefaultMutableTreeNode("child");
        parentNode.add(childNode);

        when(mockTree.getLastSelectedPathComponent()).thenReturn(childNode);
        when(mockTree.getModel()).thenReturn(mockModel);

        // Inject mocks via reflection (simplified for this context)
        try {
            Field treeField = LeftDrawer.class.getDeclaredField("leftDrawer");
            treeField.setAccessible(true);
            treeField.set(leftDrawer, mockTree);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void deleteNode_ShouldUpdateTreeStructure() {
//        leftDrawer.deleteNode(); // ToDo: Fix Test
        verify(mockModel).removeNodeFromParent(childNode);
        verify(mockModel).nodeStructureChanged(parentNode);
    }
}