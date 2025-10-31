package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.ScopeTemplateEntry;
import de.bund.zrb.model.ScopeVariableEntry;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

/**
 * Wiederverwendbares Panel für eine Liste von Scope-Einträgen
 * (entweder Variablen oder Templates).
 *
 * - Zeigt eine JTable mit Spalten Name / Expression / Description
 * - [+] fügt neuen Eintrag hinzu
 * - [x] löscht den ausgewählten Eintrag
 * - Doppelklick oder "Bearbeiten..." erlaubt Inline-Änderung in der Tabelle,
 *   weil wir das TableModel als editable auslegen.
 *
 * Nach jeder Änderung wird saveCallback.run() aufgerufen,
 * damit TestRegistry.save() passieren kann.
 *
 * Wir unterstützen zwei Typen:
 *   MODE_VARIABLES  -> List<ScopeVariableEntry>
 *   MODE_TEMPLATES  -> List<ScopeTemplateEntry>
 *
 * Für beide verwenden wir dieselben 3 Spalten.
 */
public class ScopeTablePanel extends JPanel {

    public enum Mode {
        MODE_VARIABLES,
        MODE_TEMPLATES
    }

    private final Mode mode;
    private final List<ScopeVariableEntry> varList;
    private final List<ScopeTemplateEntry> tplList;
    private final Runnable saveCallback;

    private JTable table;
    private EntryTableModel tableModel;

    public ScopeTablePanel(
            Mode mode,
            List<ScopeVariableEntry> varList,
            List<ScopeTemplateEntry> tplList,
            Runnable saveCallback
    ) {
        super(new BorderLayout(8,8));
        this.mode = mode;
        this.varList = varList;
        this.tplList = tplList;
        this.saveCallback = saveCallback;

        buildUI();
    }

    private void buildUI() {
        tableModel = new EntryTableModel();
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);

        JScrollPane scroll = new JScrollPane(table);

        JButton addBtn = new JButton("+");
        JButton delBtn = new JButton("x");
        JButton editBtn = new JButton("Bearbeiten");

        addBtn.addActionListener(e -> onAdd());
        delBtn.addActionListener(e -> onDelete());
        editBtn.addActionListener(e -> onEdit());

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnBar.add(addBtn);
        btnBar.add(editBtn);
        btnBar.add(delBtn);

        add(scroll, BorderLayout.CENTER);
        add(btnBar, BorderLayout.SOUTH);
    }

    private void onAdd() {
        String name = JOptionPane.showInputDialog(this, "Name:");
        if (name == null || name.trim().isEmpty()) return;

        String expr = JOptionPane.showInputDialog(this, "Expression (expressionRaw):");
        if (expr == null) expr = "";

        String desc = JOptionPane.showInputDialog(this, "Description (optional):");
        if (desc == null) desc = "";

        if (mode == Mode.MODE_VARIABLES) {
            ScopeVariableEntry ent = new ScopeVariableEntry();
            ent.setName(name.trim());
            ent.setExpressionRaw(expr);
            ent.setDescription(desc);
            varList.add(ent);
        } else {
            ScopeTemplateEntry ent = new ScopeTemplateEntry();
            ent.setName(name.trim());
            ent.setExpressionRaw(expr);
            ent.setDescription(desc);
            tplList.add(ent);
        }

        tableModel.fireTableDataChanged();
        saveCallback.run();
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Eintrag wirklich löschen?",
                "Löschen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        if (mode == Mode.MODE_VARIABLES) {
            if (row >= 0 && row < varList.size()) {
                varList.remove(row);
            }
        } else {
            if (row >= 0 && row < tplList.size()) {
                tplList.remove(row);
            }
        }

        tableModel.fireTableDataChanged();
        saveCallback.run();
    }

    /**
     * "Bearbeiten" macht eigentlich nichts anderes als die JTable editierbar zu machen.
     * Wir triggern hier einfach editieren der ersten Spalte des selektierten Rows.
     */
    private void onEdit() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        table.editCellAt(row, 0);
    }

    /**
     * TableModel mit 3 Spalten: Name | Expression | Description
     * Es schreibt Änderungen sofort zurück ins jeweilige Objekt
     * und ruft saveCallback.run().
     */
    private class EntryTableModel extends AbstractTableModel {

        private final String[] COLS = new String[] {
                "Name", "Expression", "Description"
        };

        @Override
        public int getRowCount() {
            if (mode == Mode.MODE_VARIABLES) {
                return varList.size();
            } else {
                return tplList.size();
            }
        }

        @Override
        public int getColumnCount() {
            return COLS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLS[column];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // alle drei Spalten editierbar
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (mode == Mode.MODE_VARIABLES) {
                ScopeVariableEntry ent = varList.get(rowIndex);
                switch (columnIndex) {
                    case 0: return ent.getName();
                    case 1: return ent.getExpressionRaw();
                    case 2: return ent.getDescription();
                }
            } else {
                ScopeTemplateEntry ent = tplList.get(rowIndex);
                switch (columnIndex) {
                    case 0: return ent.getName();
                    case 1: return ent.getExpressionRaw();
                    case 2: return ent.getDescription();
                }
            }
            return "";
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            String v = (aValue == null) ? "" : aValue.toString();
            if (mode == Mode.MODE_VARIABLES) {
                ScopeVariableEntry ent = varList.get(rowIndex);
                switch (columnIndex) {
                    case 0: ent.setName(v); break;
                    case 1: ent.setExpressionRaw(v); break;
                    case 2: ent.setDescription(v); break;
                }
            } else {
                ScopeTemplateEntry ent = tplList.get(rowIndex);
                switch (columnIndex) {
                    case 0: ent.setName(v); break;
                    case 1: ent.setExpressionRaw(v); break;
                    case 2: ent.setDescription(v); break;
                }
            }

            // sofort persistieren
            saveCallback.run();
        }
    }
}
