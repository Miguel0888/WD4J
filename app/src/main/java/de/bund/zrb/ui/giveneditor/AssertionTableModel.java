package de.bund.zrb.ui.giveneditor;

import javax.swing.table.AbstractTableModel;
import java.util.*;

/**
 * Columns: [0]=Enabled(Boolean), [1]=Name(String), [2]=Expression(String)
 * - Keep maps in sync: expressions[name] , enabled[name]
 * - Optional pinned first row: name/value locked, enabled editable.
 */
public class AssertionTableModel extends AbstractTableModel {

    private final Map<String,String> expressions;
    private final Map<String,Boolean> enabled;
    private final List<String> keys;
    private final boolean includePinnedRow;
    private final String pinnedKey;

    public AssertionTableModel(Map<String,String> expressions,
                               Map<String,Boolean> enabled,
                               boolean includePinnedRow,
                               String pinnedKey) {
        this.expressions = expressions;
        this.enabled = enabled;
        this.includePinnedRow = includePinnedRow;
        this.pinnedKey = pinnedKey;

        this.keys = new ArrayList<String>(expressions.keySet());

        // Ensure order: pinned first if present
        if (includePinnedRow && pinnedKey != null) {
            if (!keys.contains(pinnedKey)) {
                keys.add(0, pinnedKey);
            } else {
                keys.remove(pinnedKey);
                keys.add(0, pinnedKey);
            }
        }
    }

    public int getRowCount() { return keys.size(); }
    public int getColumnCount() { return 3; }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0: return "Aktiv";
            case 1: return "Name";
            case 2: return "Expression";
            default: return super.getColumnName(column);
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? Boolean.class : String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        String key = keys.get(rowIndex);
        switch (columnIndex) {
            case 0: {
                Boolean b = enabled.get(key);
                return (b != null) ? b : Boolean.TRUE;
            }
            case 1: return key;
            case 2: return expressions.get(key);
            default: return "";
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Pinned: only checkbox editable
        if (includePinnedRow && rowIndex == 0) {
            return columnIndex == 0;
        }
        return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        String oldKey = keys.get(rowIndex);
        if (columnIndex == 0) {
            boolean val = (aValue instanceof Boolean) ? ((Boolean) aValue).booleanValue() : Boolean.TRUE;
            enabled.put(oldKey, val);
            fireTableRowsUpdated(rowIndex, rowIndex);
            return;
        }

        if (columnIndex == 1) {
            // Rename key
            String newKey = (aValue == null) ? "" : String.valueOf(aValue).trim();
            if (newKey.length() == 0) return;
            if (includePinnedRow && rowIndex == 0) return; // pinned locked

            if (!newKey.equals(oldKey)) {
                if (expressions.containsKey(newKey)) return; // avoid override
                String exprVal = expressions.remove(oldKey);
                Boolean enVal = enabled.remove(oldKey);
                expressions.put(newKey, exprVal);
                enabled.put(newKey, enVal != null ? enVal : Boolean.TRUE);
                keys.set(rowIndex, newKey);
            }
            fireTableRowsUpdated(rowIndex, rowIndex);
            return;
        }

        if (columnIndex == 2) {
            if (includePinnedRow && rowIndex == 0) return; // pinned locked
            String expr = (aValue == null) ? "" : String.valueOf(aValue);
            expressions.put(oldKey, expr);
            fireTableRowsUpdated(rowIndex, rowIndex);
        }
    }

    public void addEmptyRow() {
        String base = "assertion";
        String cand = base;
        int i = 2;
        while (expressions.containsKey(cand)) {
            cand = base + "_" + i;
            i++;
        }
        expressions.put(cand, "");
        enabled.put(cand, Boolean.TRUE);
        if (includePinnedRow) {
            keys.add(1, cand);
            fireTableRowsInserted(1, 1);
        } else {
            keys.add(cand);
            int newRow = keys.size() - 1;
            fireTableRowsInserted(newRow, newRow);
        }
    }

    public void removeRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= keys.size()) return;
        if (includePinnedRow && rowIndex == 0) return; // pinned locked
        String k = keys.remove(rowIndex);
        expressions.remove(k);
        enabled.remove(k);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    public boolean canRemoveRow(int rowIndex) {
        if (!includePinnedRow) return true;
        return rowIndex != 0;
    }

    public String getKeyAt(int rowIndex) {
        return (rowIndex >= 0 && rowIndex < keys.size()) ? keys.get(rowIndex) : null;
    }
}
