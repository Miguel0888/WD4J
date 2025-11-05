package de.bund.zrb.ui.giveneditor;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Two-column TableModel for Map<String,String>.
 * Column 0 = Name (key), Column 1 = Expression (value).
 * Write changes back to the map immediately.
 *
 * Optional behavior:
 * - If includeUserRow == true, ensure a pinned first row "user".
 * - Row 0 name is read-only and cannot be removed; value is editable.
 */
public class MapTableModel extends AbstractTableModel {

    public static final String USER_KEY = "user";

    private final Map<String,String> backing;
    private final List<String> keys;
    private final boolean includeUserRow;

    /** Keep old behavior (no user row). */
    public MapTableModel(Map<String,String> backing) {
        this(backing, false);
    }

    /** New behavior toggle: includeUserRow controls the pinned "user" row. */
    public MapTableModel(Map<String,String> backing, boolean includeUserRow) {
        this.backing = backing;
        this.includeUserRow = includeUserRow;

        if (includeUserRow && backing != null && !backing.containsKey(USER_KEY)) {
            backing.put(USER_KEY, "");
        }

        this.keys = new java.util.ArrayList<String>(backing != null ? backing.keySet() : java.util.Collections.<String>emptyList());

        if (includeUserRow) {
            int idx = this.keys.indexOf(USER_KEY);
            if (idx >= 0) {
                this.keys.remove(idx);
            }
            this.keys.add(0, USER_KEY);
        }
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
            // Show "user" label on pinned row for better UX
//            if (includeUserRow && rowIndex == 0 && USER_KEY.equals(key)) {
//                return "user (beforeAll)";
//            }
            return key;
        } else if (columnIndex == 1) {
            return backing.get(key);
        }
        return "";
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Pin name cell on row 0 if user row is active
        if (includeUserRow && rowIndex == 0 && columnIndex == 0) {
            return false;
        }
        return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        String oldKey = keys.get(rowIndex);
        String val = (aValue == null) ? "" : String.valueOf(aValue);

        if (columnIndex == 0) {
            // Keep "user" name locked if pinned
            if (includeUserRow && rowIndex == 0 && USER_KEY.equals(oldKey)) {
                return;
            }
            String newKey = val.trim();
            if (newKey.length() == 0) {
                return; // Do not allow empty key
            }
            // Prevent accidental rename to "user" somewhere else
            if (includeUserRow && !"user".equals(oldKey) && USER_KEY.equals(newKey) && rowIndex != 0) {
                return;
            }
            if (!newKey.equals(oldKey)) {
                String oldValue = backing.remove(oldKey);
                // Avoid overriding existing entries
                if (!backing.containsKey(newKey)) {
                    backing.put(newKey, oldValue);
                    keys.set(rowIndex, newKey);
                } else {
                    // Key already exists -> revert (no-op)
                    backing.put(oldKey, oldValue);
                }
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
        // Insert after pinned user row if present
        int insertIndex = includeUserRow ? 1 : keys.size();
        if (includeUserRow) {
            // keep new keys after row 0
            keys.add(insertIndex, cand);
            fireTableRowsInserted(insertIndex, insertIndex);
        } else {
            keys.add(cand);
            int newRow = keys.size() - 1;
            fireTableRowsInserted(newRow, newRow);
        }
    }

    /** Remove the row at index. */
    public void removeRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= keys.size()) return;

        // Never remove pinned "user" row
        if (includeUserRow && rowIndex == 0 && USER_KEY.equals(keys.get(rowIndex))) {
            return;
        }

        String k = keys.remove(rowIndex);
        backing.remove(k);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    /** Return true if the row can be removed (never the pinned user row). */
    public boolean canRemoveRow(int rowIndex) {
        if (!includeUserRow) return true;
        if (rowIndex == 0 && USER_KEY.equals(keys.get(rowIndex))) return false;
        return true;
    }

    /** Get underlying key at row (useful for custom renderers/editors). */
    public String getKeyAt(int rowIndex) {
        return (rowIndex >= 0 && rowIndex < keys.size()) ? keys.get(rowIndex) : null;
    }
}
