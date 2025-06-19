package de.bund.zrb.ui;

import de.bund.zrb.model.TestAction;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionTable extends JTable {
    private final ActionTableModel tableModel;
    private final JPopupMenu columnMenu;

    private final Map<String, Boolean> columnVisibility = new HashMap<>();
    private final Map<TableColumn, Integer> columnWidths = new HashMap<>();

    public ActionTable() {
        this.tableModel = new ActionTableModel();
        setModel(tableModel);
        this.columnMenu = new JPopupMenu();

        configureColumns(); // 🛠️ Initiale Spaltenkonfiguration
        setUpEditors();

        // 🔥 Lauscher hinzufügen, damit sich die Spalten dynamisch aktualisieren
        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.UPDATE) {
                configureColumns(); // ✅ Spalten neu setzen
                setUpEditors(); // 🟢 Editoren erneut setzen
            }
        });
    }

    /** 🔧 Konfiguriert das Spalten-Menü und setzt den Header */
    private void configureColumns() {
        TableColumnModel columnModel = getColumnModel();
        columnModel.getColumn(0).setHeaderRenderer(new ButtonHeaderRenderer(columnMenu));
        columnModel.getColumn(0).setPreferredWidth(30);
        columnModel.getColumn(0).setMaxWidth(40);
        columnModel.getColumn(0).setMinWidth(30);
        columnModel.getColumn(0).setResizable(false);

        columnMenu.removeAll(); // 🔄 Menü leeren, um doppelte Einträge zu vermeiden

        // Spaltensteuerungs-Menü neu aufbauen
        for (int i = 1; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            String columnName = tableModel.getColumnName(i);

            // 🔥 Speichere die aktuelle Breite IMMER, auch wenn Spalte schon existiert
            columnWidths.put(column, column.getPreferredWidth());

            // 🔥 Sichtbarkeit beibehalten oder Standardwert setzen
            boolean isVisible = columnVisibility.getOrDefault(columnName, true);
            setColumnVisibility(column, isVisible);

            // 🟢 Menüeintrag hinzufügen
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(tableModel.getColumnName(i), true);
            menuItem.setSelected(isVisible);
            menuItem.addActionListener(e -> {
                boolean selected = menuItem.isSelected();
                columnVisibility.put(columnName, selected);
                setColumnVisibility(column, selected);
            });

            columnMenu.add(menuItem);
        }
    }

    private void setColumnVisibility(TableColumn column, boolean visible) {
        if (visible) {
            int originalWidth = columnWidths.getOrDefault(column, 100); // 🔥 Breite wiederherstellen
            column.setMinWidth(75);
            column.setMaxWidth(300);
            column.setPreferredWidth(originalWidth);
            column.setResizable(true);
        } else {
            column.setMinWidth(0);
            column.setMaxWidth(0);
            column.setResizable(false);
        }
    }

    /** 🟢 Setzt die Spalteneditoren für DropDowns */
    private void setUpEditors() {
        TableColumnModel columnModel = getColumnModel();

        // 🟢 Checkbox-Editor setzen
        columnModel.getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        columnModel.getColumn(0).setCellRenderer(getDefaultRenderer(Boolean.class));

        // Aktionen DropDown
        JComboBox<String> actionComboBox = new JComboBox<>(new String[]{"click", "input", "screenshot"});
        columnModel.getColumn(1).setCellEditor(new DefaultCellEditor(actionComboBox));

        // 🛠️ MouseListener für Klicks im Header hinzufügen
        JTableHeader header = getTableHeader();
        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int column = columnAtPoint(evt.getPoint());
                if (column == 0) {
                    columnMenu.show(header, evt.getX(), evt.getY()); // 🛠️ Popup-Menü anzeigen
                }
            }
        });
    }

    /** 🔧 Action hinzufügen und automatisch UI updaten */
    public void addAction(TestAction action) {
        tableModel.addAction(action);
    }

    /** 🔧 Entfernt eine Aktion */
    public void removeAction(int rowIndex) {
        tableModel.removeAction(rowIndex);
    }

    /** 🔧 Setzt neue Daten */
    public void setRowData(List<TestAction> actions) {
        tableModel.setRowData(actions);
    }

    /** 🔧 Custom Renderer für den Header mit Button */
    static class ButtonHeaderRenderer extends JLabel implements TableCellRenderer {
        private final JPopupMenu columnMenu;

        public ButtonHeaderRenderer(JPopupMenu columnMenu) {
            this.columnMenu = columnMenu;
            setText("\uD83D\uDD27"); // 🔧 Schraubenschlüssel-Symbol
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
            setBackground(new Color(230, 230, 230));
            setBorder(BorderFactory.createLineBorder(Color.GRAY));
            setToolTipText("Spalten anzeigen/ausblenden");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }
}
