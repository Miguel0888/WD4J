package de.bund.zrb.ui.giveneditor;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapTableModel extends AbstractTableModel {

    public static final String USER_KEY = "user";

    private final Map<String,String> backing;
    private final List<String> keys = new ArrayList<String>();
    private final Map<String, Boolean> enabledBacking;

    private final boolean includeUserRow;
    private final boolean includePinnedRow;
    private final String pinnedKey; // z. B. "OTP"

    public MapTableModel(Map<String,String> backing) {
        this(backing, null, false, false, null);
    }

    public MapTableModel(Map<String,String> backing, boolean includeUserRow) {
        this(backing, null, includeUserRow, false, null);
    }

    public MapTableModel(Map<String,String> backing,
                         Map<String, Boolean> backingEnabled,
                         boolean includeUserRow,
                         boolean includePinnedRow,
                         String pinnedKey) {
        this.backing = backing;
        this.enabledBacking = (backingEnabled != null)
                ? backingEnabled
                : new java.util.LinkedHashMap<String, Boolean>();
        this.includeUserRow = includeUserRow;
        this.includePinnedRow = includePinnedRow;
        this.pinnedKey = pinnedKey;

        if (backing != null) keys.addAll(backing.keySet());

        // Pinned first
        if (includePinnedRow && pinnedKey != null) {
            int pos = keys.indexOf(pinnedKey);
            if (pos >= 0) keys.remove(pos);
            keys.add(0, pinnedKey);
        } else if (includeUserRow) {
            int pos = keys.indexOf(USER_KEY);
            if (pos >= 0) keys.remove(pos);
            keys.add(0, USER_KEY);
        }
    }

    public int getRowCount() { return keys.size(); }
    public int getColumnCount() { return 3; }
    public String getColumnName(int c) {
        switch (c) {
            case 0: return "Enabled";
            case 1: return "Name";
            default: return "Expression";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? Boolean.class : String.class;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        String key = keys.get(rowIndex);
        if (columnIndex == 0) {
            Boolean val = enabledBacking != null ? enabledBacking.get(key) : null;
            return val == null ? Boolean.TRUE : val;
        }
        if (columnIndex == 1) return key;
        return backing != null ? backing.get(key) : "";
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            // allow toggling enabled for all rows
            return true;
        }
        // Pinned erste Zeile: sowohl Name (0) als auch Expression (1) sperren
        if (rowIndex == 0 && includePinnedRow && columnIndex >= 1) {
            return false;
        }
        if (rowIndex == 0 && columnIndex == 1 && includeUserRow) {
            return false; // user variable name is not editable
        }
        return true;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (backing == null) return;
        String oldKey = keys.get(rowIndex);
        String val = (aValue == null) ? "" : String.valueOf(aValue);

        if (columnIndex == 0) {
            boolean enabled = (aValue instanceof Boolean)
                    ? ((Boolean) aValue).booleanValue()
                    : Boolean.parseBoolean(val);
            if (enabledBacking != null) {
                enabledBacking.put(oldKey, Boolean.valueOf(enabled));
            }
        } else if (columnIndex == 1) {
            if (rowIndex == 0 && (includePinnedRow || includeUserRow)) return; // Name gesperrt
            String newKey = val.trim();
            if (newKey.length() == 0) return;
            if (!newKey.equals(oldKey)) {
                String oldValue = backing.remove(oldKey);
                Boolean enabledVal = enabledBacking != null ? enabledBacking.remove(oldKey) : null;
                if (!backing.containsKey(newKey)) {
                    backing.put(newKey, oldValue);
                    keys.set(rowIndex, newKey);
                    if (enabledBacking != null) {
                        enabledBacking.put(newKey, enabledVal != null ? enabledVal : Boolean.TRUE);
                    }
                } else {
                    backing.put(oldKey, oldValue);
                    if (enabledBacking != null && enabledVal != null) {
                        enabledBacking.put(oldKey, enabledVal);
                    }
                }
            }
        } else {
            backing.put(oldKey, val);
        }
        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    public void addEmptyRow() {
        if (backing == null) return;
        String base = "neu";
        String cand = base;
        int i = 2;
        while (backing.containsKey(cand)) {
            cand = base + "_" + i;
            i++;
        }
        backing.put(cand, "");
        if (enabledBacking != null) {
            enabledBacking.put(cand, Boolean.TRUE);
        }
        int insertIndex = (includePinnedRow || includeUserRow) ? 1 : keys.size();
        keys.add(insertIndex, cand);
        fireTableRowsInserted(insertIndex, insertIndex);
    }

    public void removeRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= keys.size()) return;
        if (rowIndex == 0 && (includePinnedRow || includeUserRow)) return; // gepinnt → nicht löschen
        String k = keys.remove(rowIndex);
        if (backing != null) backing.remove(k);
        if (enabledBacking != null) enabledBacking.remove(k);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    public boolean canRemoveRow(int rowIndex) {
        if (rowIndex == 0 && (includePinnedRow || includeUserRow)) return false;
        return true;
    }

    public String getKeyAt(int rowIndex) {
        return (rowIndex >= 0 && rowIndex < keys.size()) ? keys.get(rowIndex) : null;
    }
}
