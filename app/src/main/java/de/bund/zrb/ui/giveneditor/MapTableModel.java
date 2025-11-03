// src/main/java/de/bund/zrb/ui/giveneditor/MapTableModel.java
package de.bund.zrb.ui.giveneditor;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Two-column TableModel for Map<String,String>.
 * Column 0 = Name (key), Column 1 = Expression (value).
 * Write changes back to the map immediately.
 */
public class MapTableModel extends AbstractTableModel {

    private final Map<String,String> backing;
    private final List<String> keys;

    public MapTableModel(Map<String,String> backing) {
        this.backing = backing;
        this.keys = new ArrayList<String>(backing.keySet());
    }

    @Override
    public int getRowCount() {
        return keys.size();
    }

    @Override
    public int getColumnCount() {
        return 2; // Name | Expression
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) return "Name";
        if (column == 1) return "Expression";
        return super.getColumnName(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        String key = keys.get(rowIndex);
        if (columnIndex == 0) {
            return key;
        } else if (columnIndex == 1) {
            return backing.get(key);
        }
        return "";
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        String oldKey = keys.get(rowIndex);
        String val = (aValue == null) ? "" : String.valueOf(aValue);

        if (columnIndex == 0) {
            String newKey = val.trim();
            if (newKey.length() == 0) {
                return; // Do not allow empty key
            }
            if (!newKey.equals(oldKey)) {
                String oldValue = backing.remove(oldKey);
                backing.put(newKey, oldValue);
                keys.set(rowIndex, newKey);
            }
        } else if (columnIndex == 1) {
            backing.put(oldKey, val);
        }

        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    /** Add a new empty row with a unique "neu" key. */
    public void addEmptyRow() {
        String base = "neu";
        String cand = base;
        int i = 2;
        while (backing.containsKey(cand)) {
            cand = base + "_" + i;
            i++;
        }
        backing.put(cand, "");
        keys.add(cand);
        int newRow = keys.size() - 1;
        fireTableRowsInserted(newRow, newRow);
    }

    /** Remove the row at index. */
    public void removeRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= keys.size()) return;
        String k = keys.remove(rowIndex);
        backing.remove(k);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }
}
