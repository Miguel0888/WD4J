package de.bund.zrb.ui.giveneditor;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ein 2-Spalten-TableModel für eine Map<String,String>.
 * Spalte 0 = Name (Key)
 * Spalte 1 = Expression (Value)
 *
 * Änderungen im TableModel spiegeln sich direkt in der Map.
 */
public class MapTableModel extends AbstractTableModel {

    private final Map<String,String> backing;
    // Wir halten eine sortierte/iterierbare Ansicht der Keys,
    // damit JTable eine stabile Reihenfolge hat.
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
            // Key (Name) wurde geändert -> wir müssen umbenennen
            String newKey = val.trim();
            if (newKey.length() == 0) {
                // leeren Namen nicht zulassen -> ignoriere
                return;
            }
            if (!newKey.equals(oldKey)) {
                String oldValue = backing.remove(oldKey);
                backing.put(newKey, oldValue);
                keys.set(rowIndex, newKey);
            }
        } else if (columnIndex == 1) {
            // Expression geändert
            backing.put(oldKey, val);
        }

        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    /**
     * Neue Zeile hinzufügen: legt "neu" als Key an, falls frei.
     */
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

    /**
     * Ausgewählte Zeile löschen.
     */
    public void removeRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= keys.size()) return;
        String k = keys.remove(rowIndex);
        backing.remove(k);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }
}
