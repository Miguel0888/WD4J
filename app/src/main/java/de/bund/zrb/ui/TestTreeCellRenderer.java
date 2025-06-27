package de.bund.zrb.ui;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Renders test nodes with pass/fail colors.
 */
public class TestTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        JLabel label = (JLabel) super.getTreeCellRendererComponent(
                tree, value, sel, expanded, leaf, row, hasFocus);

        if (value instanceof TestNode) {
            TestNode node = (TestNode) value;
            if (node.getStatus() == TestNode.Status.PASSED) {
                label.setForeground(Color.GREEN.darker());
                label.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
            } else if (node.getStatus() == TestNode.Status.FAILED) {
                label.setForeground(Color.RED);
                label.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
            } else {
                label.setForeground(Color.BLACK);
                label.setIcon(null);
            }
        }

        return label;
    }
}
