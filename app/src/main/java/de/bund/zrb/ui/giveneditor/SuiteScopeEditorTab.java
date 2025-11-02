package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.Precondtion;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.TestRegistry;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

/**
 * Editor für den Suite-Scope.
 *
 * Tabs:
 *  - BeforeAll
 *  - BeforeEach
 *  - Templates
 *
 * Oben rechts: Speichern-Button, der aktuell einfach TestRegistry.save() aufruft.
 * Die Tabellen haben + und – zum Hinzufügen/Entfernen.
 */
public class SuiteScopeEditorTab extends JPanel {

    private final TestSuite suite;
    private final JTabbedPane innerTabs = new JTabbedPane();

    public SuiteScopeEditorTab(TestSuite suite) {
        super(new BorderLayout());
        this.suite = suite;

        // ===== Header (Titel, Description, Save Button) =====
        JPanel header = new JPanel(new BorderLayout());

        // linker Block mit Titel + Beschreibung
        JPanel textBlock = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Suite-Scope: " + safe(suite.getName()), SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

        JTextArea desc = new JTextArea();
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setEditable(false);
        desc.setOpaque(false);
        desc.setText(safe(suite.getDescription()));

        textBlock.add(title, BorderLayout.NORTH);
        textBlock.add(desc, BorderLayout.CENTER);

        textBlock.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // rechter Block mit Save-Button
        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("💾 Speichern");
        saveBtn.setToolTipText("Änderungen dieser Suite in tests.json schreiben");
        saveBtn.addActionListener(e -> {
            TestRegistry.getInstance().save();
            JOptionPane.showMessageDialog(
                    SuiteScopeEditorTab.this,
                    "Gespeichert.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
        savePanel.add(saveBtn);
        savePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        header.add(textBlock, BorderLayout.CENTER);
        header.add(savePanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ===== Inhalt-Tabs (BeforeAll, BeforeEach, Templates) =====
        innerTabs.addTab("BeforeAll",   buildTablePanel(suite.getBeforeAll(),   "BeforeAll"));
        innerTabs.addTab("BeforeEach",  buildTablePanel(suite.getBeforeEach(),  "BeforeEach"));
        innerTabs.addTab("Templates",   buildTablePanel(suite.getTemplates(),   "Templates"));

        add(innerTabs, BorderLayout.CENTER);
    }

    private JPanel buildTablePanel(List<Precondtion> data, String scopeName) {
        JPanel panel = new JPanel(new BorderLayout());

        GivenTableModel model = new GivenTableModel(data);
        JTable table = new JTable(model);

        // Toolbar über der Tabelle
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton addBtn = new JButton("+");
        addBtn.setToolTipText(scopeName + " Eintrag hinzufügen");
        addBtn.addActionListener(e -> {
            data.add(new Precondtion(
                    "preconditionRef",
                    "name=<neu>&expressionRaw="
            ));
            model.fireTableDataChanged();
        });

        JButton delBtn = new JButton("–");
        delBtn.setToolTipText("Ausgewählte Zeile löschen");
        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && row < data.size()) {
                data.remove(row);
                model.fireTableDataChanged();
            }
        });

        JButton saveBtn = new JButton("💾");
        saveBtn.setToolTipText("Speichern");
        saveBtn.addActionListener(e -> {
            TestRegistry.getInstance().save();
            JOptionPane.showMessageDialog(
                    SuiteScopeEditorTab.this,
                    "Gespeichert.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        bar.add(addBtn);
        bar.add(delBtn);
        bar.addSeparator();
        bar.add(saveBtn);

        panel.add(bar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    private static String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "" : s.trim();
    }

    /**
     * TableModel für Name/Expression. Identische Logik wie zuvor,
     * plus direkte Bearbeitung (editable true).
     */
    private static class GivenTableModel extends AbstractTableModel {

        private final List<Precondtion> rows;

        public GivenTableModel(List<Precondtion> rows) {
            this.rows = rows;
        }

        @Override
        public int getRowCount() {
            return rows.size();
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
            Precondtion gc = rows.get(rowIndex);
            java.util.Map<String,String> map = parseValueMap(gc.getValue());

            if (columnIndex == 0) {
                return map.getOrDefault("name", "");
            }
            if (columnIndex == 1) {
                return map.getOrDefault("expressionRaw", "");
            }
            return "";
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Precondtion gc = rows.get(rowIndex);
            java.util.Map<String,String> map = parseValueMap(gc.getValue());

            String val = (aValue == null) ? "" : String.valueOf(aValue);

            if (columnIndex == 0) {
                map.put("name", val);
            } else if (columnIndex == 1) {
                map.put("expressionRaw", val);
            }

            gc.setValue(serializeValueMap(map));
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        private java.util.Map<String,String> parseValueMap(String raw) {
            java.util.Map<String,String> result = new java.util.LinkedHashMap<>();
            if (raw != null && raw.contains("=")) {
                String[] pairs = raw.split("&");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        result.put(kv[0], kv[1]);
                    }
                }
            }
            return result;
        }

        private String serializeValueMap(java.util.Map<String,String> map) {
            StringBuilder sb = new StringBuilder();
            for (java.util.Map.Entry<String,String> e : map.entrySet()) {
                if (sb.length() > 0) sb.append("&");
                sb.append(e.getKey()).append("=").append(e.getValue());
            }
            return sb.toString();
        }
    }
}
