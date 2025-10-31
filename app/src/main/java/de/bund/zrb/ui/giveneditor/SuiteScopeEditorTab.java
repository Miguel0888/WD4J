package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.TestSuite;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

/**
 * Editor für den Suite-Scope.
 *
 * Tabs:
 *  - BeforeAll   (Variablen, einmalig vor Suite)
 *  - BeforeEach  (Variablen, vor jedem Case)
 *  - Templates   (Funktionszeiger/lazy)
 *
 * Aktuell: Nur Anzeige/Bearbeiten in Tabellenform.
 * Persistierung passiert später in Step "Speichern beim Edit".
 */
public class SuiteScopeEditorTab extends JPanel {

    private final TestSuite suite;
    private final JTabbedPane innerTabs = new JTabbedPane();

    public SuiteScopeEditorTab(TestSuite suite) {
        super(new BorderLayout());
        this.suite = suite;

        // Oben Suite-Name/Description zur Orientierung
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Suite-Scope: " + safe(suite.getName()), SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

        JTextArea desc = new JTextArea();
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setEditable(false);
        desc.setOpaque(false);
        desc.setText(safe(suite.getDescription()));

        header.add(title, BorderLayout.NORTH);
        header.add(desc, BorderLayout.CENTER);
        header.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        add(header, BorderLayout.NORTH);

        // Tabellen-Tabs anlegen
        innerTabs.addTab("BeforeAll",  buildTablePanel(suite.getBeforeAll(),  "BeforeAll"));
        innerTabs.addTab("BeforeEach", buildTablePanel(suite.getBeforeEach(), "BeforeEach"));
        innerTabs.addTab("Templates",  buildTablePanel(suite.getTemplates(),  "Templates"));

        add(innerTabs, BorderLayout.CENTER);
    }

    private JPanel buildTablePanel(List<GivenCondition> data, String scopeName) {
        JPanel panel = new JPanel(new BorderLayout());

        GivenTableModel model = new GivenTableModel(data);
        JTable table = new JTable(model);

        // simple toolbar oben: Add/Delete (noch ohne echte Logik save/persist)
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton addBtn = new JButton("+");
        addBtn.setToolTipText(scopeName + " Variable/Template hinzufügen");
        addBtn.addActionListener(e -> {
            // naive Default-Zeile
            data.add(new GivenCondition(
                    "preconditionRef", // type lassen wir erstmal wie gehabt,
                    // später unterscheiden wir var vs template
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

        bar.add(addBtn);
        bar.add(delBtn);

        panel.add(bar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    private static String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "" : s.trim();
    }

    /**
     * Sehr einfaches TableModel:
     * - Spalte 0: Name (aus GivenCondition.value -> key "name")
     * - Spalte 1: Expression (aus GivenCondition.value -> key "expressionRaw")
     *
     * Wir gehen davon aus, dass GivenCondition aktuell "type" + "value" hat,
     * wobei value so gespeichert ist, wie in deinem Code ("key1=...&key2=...").
     *
     * Für jetzt: wir parsen/serialisieren ganz primitiv genauso wie dein
     * GivenConditionEditorTab.parseValueMap()/serializeValueMap().
     */
    private static class GivenTableModel extends AbstractTableModel {

        private final List<GivenCondition> rows;

        public GivenTableModel(List<GivenCondition> rows) {
            this.rows = rows;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 2; // Name, Expression
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) return "Name";
            if (column == 1) return "Expression";
            return super.getColumnName(column);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            GivenCondition gc = rows.get(rowIndex);
            java.util.Map<String,String> map = parseValueMap(gc.getValue());

            if (columnIndex == 0) {
                // "Name"
                return map.getOrDefault("name", "");
            }
            if (columnIndex == 1) {
                // "Expression"
                return map.getOrDefault("expressionRaw", "");
            }
            return "";
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true; // direkt editierbar
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            GivenCondition gc = rows.get(rowIndex);
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

        // --- copy of your tiny helper logic ---

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
