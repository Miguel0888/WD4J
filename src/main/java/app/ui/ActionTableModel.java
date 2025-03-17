package app.ui;

import app.dto.TestAction;
import wd4j.helper.RecorderService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.util.List;

public class ActionTableModel extends AbstractTableModel {
    private final List<TestAction> actions;
    private final String[] columnNames = {"✔", "Aktion", "Locator-Typ", "Selektor", "Wartezeit"};

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
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) return Boolean.class; // Checkbox-Spalte als Boolean
        if (columnIndex == 4) return Integer.class; // Wartezeit als Integer
        return String.class; // Andere Spalten als String
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        TestAction action = actions.get(rowIndex);
        switch (columnIndex) {
            case 0: return action.isSelected(); // Checkbox-Wert (Boolean)
            case 1: return action.getAction();
            case 2: return action.getLocatorType();
            case 3: return action.getSelectedSelector();
            case 4: return action.getTimeout();
            default: return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        TestAction action = actions.get(rowIndex);
        switch (columnIndex) {
            case 0: action.setSelected((Boolean) value); break; // Checkbox-Status setzen
            case 1: action.setAction((String) value); break;
            case 2: action.setLocatorType((String) value); break;
            case 3: action.setSelectedSelector((String) value); break;
            case 4: action.setTimeout((Integer) value); break;
        }
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public List<TestAction> getActions() {
        return actions;
    }

    public void addAction(TestAction action) {
        actions.add(action);
        fireTableRowsInserted(actions.size() - 1, actions.size() - 1);
    }

    public void removeAction(int rowIndex) {
        actions.remove(rowIndex);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    /** 🟢 Setzt die Spalteneditoren für DropDowns */
    public void setUpEditors(JTable table) {
        // 🟢 Checkbox-Editor setzen (Boolean-Werte)
        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        table.getColumnModel().getColumn(0).setCellRenderer(table.getDefaultRenderer(Boolean.class));

        // Aktionen DropDown
        JComboBox<String> actionComboBox = new JComboBox<>(new String[]{"click", "input", "screenshot"});
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(actionComboBox));

        // Locator-Typen DropDown
        JComboBox<String> locatorTypeComboBox = new JComboBox<>(new String[]{"css", "xpath", "id", "text", "role", "label", "placeholder", "altText"});
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(locatorTypeComboBox));

        // Selektor DropDown dynamisch befüllen
        JComboBox<String> selectorComboBox = new JComboBox<>();
        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(selectorComboBox));

        // Selektoren dynamisch nachladen
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                TestAction action = getActions().get(row);
                List<String> suggestions = RecorderService.getInstance().getSelectorAlternatives(action.getSelectedSelector());
                selectorComboBox.removeAllItems();
                for (String suggestion : suggestions) {
                    selectorComboBox.addItem(suggestion);
                }
            }
        });

        // Spaltenausrichtung anpassen (zentriert für "Wartezeit")
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
    }

    public void setRowData(List<TestAction> when) {
        actions.clear();
        actions.addAll(when);
        fireTableDataChanged();
    }
}
