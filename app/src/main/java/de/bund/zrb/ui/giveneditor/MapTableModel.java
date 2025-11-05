package de.bund.zrb.ui.giveneditor;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapTableModel extends AbstractTableModel {

    public static final String USER_KEY = "user";

    private final Map<String,String> backing;
    private final List<String> keys = new ArrayList<String>();

    private final boolean includeUserRow;
    private final boolean includePinnedRow;
    private final String pinnedKey; // z. B. "OTP"

    public MapTableModel(Map<String,String> backing) {
        this(backing, false, false, null);
    }

    public MapTableModel(Map<String,String> backing, boolean includeUserRow) {
        this(backing, includeUserRow, false, null);
    }

    public MapTableModel(Map<String,String> backing,
                         boolean includeUserRow,
                         boolean includePinnedRow,
                         String pinnedKey) {
        this.backing = backing;
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
    public int getColumnCount() { return 2; }
    public String getColumnName(int c) { return c == 0 ? "Name" : "Expression"; }

    public Object getValueAt(int rowIndex, int columnIndex) {
        String key = keys.get(rowIndex);
        if (columnIndex == 0) return key;
        return backing != null ? backing.get(key) : "";
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Pinned erste Zeile: sowohl Name (0) als auch Expression (1) sperren
        if (rowIndex == 0 && (includePinnedRow || includeUserRow)) {
            return false;
        }
        return true;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (backing == null) return;
        String oldKey = keys.get(rowIndex);
        String val = (aValue == null) ? "" : String.valueOf(aValue);

        if (columnIndex == 0) {
            if (rowIndex == 0 && (includePinnedRow || includeUserRow)) return; // Name gesperrt
            String newKey = val.trim();
            if (newKey.length() == 0) return;
            if (!newKey.equals(oldKey)) {
                String oldValue = backing.remove(oldKey);
                if (!backing.containsKey(newKey)) {
                    backing.put(newKey, oldValue);
                    keys.set(rowIndex, newKey);
                } else {
                    backing.put(oldKey, oldValue);
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
        int insertIndex = (includePinnedRow || includeUserRow) ? 1 : keys.size();
        keys.add(insertIndex, cand);
        fireTableRowsInserted(insertIndex, insertIndex);
    }

    public void removeRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= keys.size()) return;
        if (rowIndex == 0 && (includePinnedRow || includeUserRow)) return; // gepinnt → nicht löschen
        String k = keys.remove(rowIndex);
        if (backing != null) backing.remove(k);
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
