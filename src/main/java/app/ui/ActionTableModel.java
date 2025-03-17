package app.ui;

import app.dto.TestAction;
import javax.swing.table.AbstractTableModel;
import java.util.List;

public class ActionTableModel extends AbstractTableModel {
    private final String[] columnNames = {"Aktion", "Locator-Typ", "Selektor/Text", "Timeout", "Wert"};
    private final List<TestAction> actions;

    public ActionTableModel(List<TestAction> actions) {
        this.actions = actions;
    }

    @Override
    public int getRowCount() {
        return actions.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        TestAction action = actions.get(rowIndex);
        switch (columnIndex) {
            case 0: return action.getAction();
            case 1: return action.getLocatorType();
            case 2: return action.getSelectedSelector();
            case 3: return action.getTimeout();
            case 4: return action.getValue();
            default: return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true; // Alle Zellen sind editierbar
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        TestAction action = actions.get(rowIndex);
        if (aValue == null) return;

        switch (columnIndex) {
            case 0: action.setAction(aValue.toString()); break;
            case 1: action.setLocatorType(aValue.toString()); break;
            case 2: action.setSelectedSelector(aValue.toString()); break;
            case 3:
                try {
                    action.setTimeout(Integer.parseInt(aValue.toString()));
                } catch (NumberFormatException e) {
                    action.setTimeout(3000); // Standardwert
                }
                break;
            case 4: action.setValue(aValue.toString()); break;
        }

        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public void addAction(TestAction action) {
        actions.add(action);
        fireTableRowsInserted(actions.size() - 1, actions.size() - 1);
    }

    public void removeAction(int rowIndex) {
        actions.remove(rowIndex);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    public List<TestAction> getActions() {
        return actions;
    }
}
