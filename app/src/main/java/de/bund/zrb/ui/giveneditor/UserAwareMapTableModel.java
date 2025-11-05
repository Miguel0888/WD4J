package de.bund.zrb.ui.giveneditor;

import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;
import java.util.Map;

/**
 * Wrap a Map<String,String> so that:
 * - row 0 is always the "user" entry
 * - name at row 0 is read-only (always "user")
 * - row 0 cannot be removed
 * - value at row 0 is editable (via a combo editor in the JTable)
 */
public final class UserAwareMapTableModel implements TableModel {

    private final MapTableModel delegate;
    private final Map<String,String> backing;

    public static final String USER_KEY = "user";

    public UserAwareMapTableModel(Map<String,String> backing) {
        // Ensure presence of "user" key; default empty string (=> leerer Eintrag)
        if (backing != null && !backing.containsKey(USER_KEY)) {
            backing.put(USER_KEY, "");
        }
        this.backing = backing;
        this.delegate = new MapTableModel(backing);
        // Delegate hält nun die Keys (inkl. "user") – wir sorgen nur für UI-Regeln
    }

    // --- TableModel delegations with row-0 special handling ---

    @Override public int getRowCount() {
        return delegate.getRowCount();
    }

    @Override public int getColumnCount() {
        return delegate.getColumnCount();
    }

    @Override public String getColumnName(int columnIndex) {
        return delegate.getColumnName(columnIndex);
    }

    @Override public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Row 0: "user" → left cell not editable; right cell editable
        if (rowIndex == 0) {
            return columnIndex == 1;
        }
        return delegate.isCellEditable(rowIndex, columnIndex);
    }

    @Override public Object getValueAt(int rowIndex, int columnIndex) {
        // We must ensure that row 0 returns the "user" entry consistently.
        // MapTableModel hält die Reihenfolge intern; um robust zu sein, greifen wir direkt zu.
        if (rowIndex == 0) {
            if (columnIndex == 0) return USER_KEY;
            if (columnIndex == 1) return backing.get(USER_KEY);
        }
        return delegate.getValueAt(rowIndex, columnIndex);
    }

    @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (rowIndex == 0) {
            // Only value column may change; name is fixed.
            if (columnIndex == 1) {
                String v = aValue == null ? "" : String.valueOf(aValue);
                backing.put(USER_KEY, v);
                // inform listeners for row 0
                delegate.fireTableRowsUpdated(0, 0);
            }
            return;
        }
        // Prevent renaming other rows to "user" accidentally
        if (columnIndex == 0) {
            String newKey = aValue == null ? "" : String.valueOf(aValue).trim();
            if ("user".equalsIgnoreCase(newKey) && rowIndex != 0) {
                // ignore rename to "user" outside row 0
                return;
            }
        }
        delegate.setValueAt(aValue, rowIndex, columnIndex);
    }

    @Override public void addTableModelListener(TableModelListener l) {
        delegate.addTableModelListener(l);
    }

    @Override public void removeTableModelListener(TableModelListener l) {
        delegate.removeTableModelListener(l);
    }

    // --- Helper to block deletion of row 0 from UI buttons ---

    /** Return true if the given row can be safely removed (never row 0). */
    public boolean canRemoveRow(int rowIndex) {
        return rowIndex > 0;
    }

    /** Remove row if allowed; no-op for row 0. */
    public void removeRowIfAllowed(int rowIndex) {
        if (rowIndex <= 0) return; // do not remove "user"
        delegate.removeRow(rowIndex);
    }

    /** Expose delegate for add-row functionality, which remains unchanged. */
    public void addEmptyRow() {
        delegate.addEmptyRow();
    }
}
