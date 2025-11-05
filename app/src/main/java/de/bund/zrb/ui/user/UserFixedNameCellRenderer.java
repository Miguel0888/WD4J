package de.bund.zrb.ui.user;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Render fixed left cell as disabled/locked. */
public final class UserFixedNameCellRenderer extends DefaultTableCellRenderer {

    private final Icon lock;

    public UserFixedNameCellRenderer() {
        this.lock = UIManager.getIcon("FileView.computerIcon"); // simple neutral icon; replace if you have a lock icon
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (row == 0 && column == 0) {
            c.setForeground(isSelected ? table.getSelectionForeground() : Color.GRAY);
            c.setFont(c.getFont().deriveFont(Font.ITALIC));
            c.setIcon(lock);
        } else {
            c.setIcon(null);
            c.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            c.setFont(c.getFont().deriveFont(Font.PLAIN));
        }
        return c;
    }
}
