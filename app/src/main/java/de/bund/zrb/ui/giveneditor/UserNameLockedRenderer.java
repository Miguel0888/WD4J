package de.bund.zrb.ui.giveneditor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Gray/locked look for the fixed 'user' name cell at row 0, column 0. */
public final class UserNameLockedRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        // Do NOT change text; just style it
        c.setForeground(isSelected ? table.getSelectionForeground() : Color.GRAY);
        c.setFont(c.getFont().deriveFont(Font.ITALIC));
        return c;
    }
}
