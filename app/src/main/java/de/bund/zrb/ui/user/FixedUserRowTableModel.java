package de.bund.zrb.ui.user;

import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;

/** Decorate an existing variables TableModel with a fixed first row: "User (beforeAll)". */
public final class FixedUserRowTableModel implements TableModel {

    private final TableModel delegate;
    private final Object scopeNode; // RootNode | TestSuite | TestCase

    public FixedUserRowTableModel(TableModel delegate, Object scopeNode) {
        this.delegate = delegate;
        this.scopeNode = scopeNode;
    }

    public int getRowCount() {
        return 1 + delegate.getRowCount();
    }

    public int getColumnCount() {
        return delegate.getColumnCount();
    }

    public String getColumnName(int columnIndex) {
        return delegate.getColumnName(columnIndex);
    }

    public Class<?> getColumnClass(int columnIndex) {
        return delegate.getColumnClass(columnIndex);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (rowIndex == 0) {
            // Left name cell not editable; right value cell editable via dropdown.
            return columnIndex == 1;
        }
        return delegate.isCellEditable(rowIndex - 1, columnIndex);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex == 0) {
            return columnIndex == 0 ? "User (beforeAll)" : UserAccessor.readUser(scopeNode);
        }
        return delegate.getValueAt(rowIndex - 1, columnIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (rowIndex == 0 && columnIndex == 1) {
            String v = (aValue != null) ? String.valueOf(aValue) : null;
            UserAccessor.writeUser(scopeNode, v);
            return;
        }
        delegate.setValueAt(aValue, rowIndex - 1, columnIndex);
    }

    public void addTableModelListener(TableModelListener l) { delegate.addTableModelListener(l); }

    public void removeTableModelListener(TableModelListener l) { delegate.removeTableModelListener(l); }
}
