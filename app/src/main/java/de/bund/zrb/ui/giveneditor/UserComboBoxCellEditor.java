package de.bund.zrb.ui.giveneditor;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.List;

public final class UserComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor {

    public interface ChoicesProvider {
        List<String> getUsers();
    }

    private final JComboBox<String> combo;
    private final ChoicesProvider provider;

    public UserComboBoxCellEditor(ChoicesProvider provider) {
        this.provider = provider;
        this.combo = new JComboBox<String>();
        this.combo.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
        this.combo.setEditable(false);
        rebuildItems();
    }

    private void rebuildItems() {
        this.combo.removeAllItems();
        // empty first (means: kein Wert → später Vererbung bzw. leerer String in Map)
        this.combo.addItem("");
        List<String> base = provider != null ? provider.getUsers() : null;
        if (base != null) {
            for (int i = 0; i < base.size(); i++) {
                String u = base.get(i);
                if (u != null) {
                    String t = u.trim();
                    if (t.length() > 0) this.combo.addItem(t);
                }
            }
        }
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        if (combo.getItemCount() == 0) {
            rebuildItems();
        }
        String v = value == null ? "" : String.valueOf(value);
        combo.setSelectedItem(v);
        return combo;
    }

    @Override
    public Object getCellEditorValue() {
        Object sel = combo.getSelectedItem();
        if (sel == null) return "";
        return String.valueOf(sel);
    }
}
