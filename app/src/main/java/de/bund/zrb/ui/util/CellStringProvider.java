package de.bund.zrb.ui.util;

import javax.swing.*;
import javax.swing.table.TableModel;

/** Provide string representation for a table cell. */
public interface CellStringProvider {
    /** Return string for given cell; never return null. */
    String toCellString(JTable table, int rowView, int colView);

    /** Default provider using model value and toString(). */
    CellStringProvider DEFAULT = new CellStringProvider() {
        @Override
        public String toCellString(JTable table, int rowView, int colView) {
            int rowModel = table.convertRowIndexToModel(rowView);
            int colModel = table.convertColumnIndexToModel(colView);
            TableModel m = table.getModel();
            Object v = m.getValueAt(rowModel, colModel);
            return v == null ? "" : String.valueOf(v);
        }
    };
}
