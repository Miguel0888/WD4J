// src/main/java/de/bund/zrb/ui/giveneditor/MapTablePanel.java
package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionMetadata;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.RegexPatternRegistry;
import de.bund.zrb.runtime.ExpressionRegistryImpl;
import de.bund.zrb.ui.celleditors.DescribedItem;
import de.bund.zrb.ui.celleditors.ExpressionCellEditor;
import de.bund.zrb.model.RootNode;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.model.TestCase;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public class MapTablePanel extends JPanel {

    public MapTablePanel(final Map<String,String> backing, final String scopeName) {
        super(new BorderLayout());

        final MapTableModel model = new MapTableModel(backing);
        final JTable table = new JTable(model);

        // Table UX
        table.setFillsViewportHeight(true);
        table.setSurrendersFocusOnKeystroke(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // Ensure at least 3-line row height
        int fmH = table.getFontMetrics(table.getFont()).getHeight();
        table.setRowHeight(Math.max(table.getRowHeight(), 3 * fmH + 8));

        // Column sizing for Expression
        if (table.getColumnModel().getColumnCount() > 1) {
            table.getColumnModel().getColumn(1).setPreferredWidth(480);
        }

        // ---- Suppliers verdrahten ----

        // Variablen: vereinige alle Keys aus Root/Suites/Cases + aktuelle Map (backing)
        Supplier<List<String>> varSupplier = new Supplier<List<String>>() {
            @Override public List<String> get() {
                java.util.Set<String> all = new java.util.LinkedHashSet<String>();

                // Aktuelles Backing (lokaler Kontext hat Priorität in der Anzeige)
                if (backing != null) {
                    all.addAll(backing.keySet());
                }

                // Global: Root
                RootNode root = TestRegistry.getInstance().getRoot();
                if (root != null) {
                    if (root.getBeforeAll()   != null) all.addAll(root.getBeforeAll().keySet());
                    if (root.getBeforeEach()  != null) all.addAll(root.getBeforeEach().keySet());
                    if (root.getTemplates()   != null) all.addAll(root.getTemplates().keySet());
                }

                // Suiten + Cases
                java.util.List<TestSuite> suites = TestRegistry.getInstance().getAll();
                if (suites != null) {
                    for (int si = 0; si < suites.size(); si++) {
                        TestSuite s = suites.get(si);
                        if (s.getBeforeAll()   != null) all.addAll(s.getBeforeAll().keySet());
                        if (s.getBeforeEach()  != null) all.addAll(s.getBeforeEach().keySet());
                        if (s.getTemplates()   != null) all.addAll(s.getTemplates().keySet());

                        java.util.List<TestCase> cases = s.getTestCases();
                        if (cases != null) {
                            for (int ci = 0; ci < cases.size(); ci++) {
                                TestCase tc = cases.get(ci);
                                if (tc.getBefore()    != null) all.addAll(tc.getBefore().keySet());
                                if (tc.getTemplates() != null) all.addAll(tc.getTemplates().keySet());
                            }
                        }
                    }
                }

                java.util.List<String> sorted = new java.util.ArrayList<String>(all);
                java.util.Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
                return sorted;
            }
        };

// Funktionen: Builtins direkt, User-Funktionen aus Metadaten (ohne Kompilierung im EDT)
        Supplier<Map<String, DescribedItem>> fnSupplier = new Supplier<Map<String, DescribedItem>>() {
            @Override public Map<String, DescribedItem> get() {
                Map<String, DescribedItem> out = new LinkedHashMap<String, DescribedItem>();
                ExpressionRegistryImpl reg = ExpressionRegistryImpl.getInstance();

                java.util.Set<String> keys = reg.getKeys();
                java.util.List<String> sorted = new java.util.ArrayList<String>(keys);
                java.util.Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);

                for (int i = 0; i < sorted.size(); i++) {
                    final String name = sorted.get(i);

                    // Builtins: echte Funktion liefern (implementiert DescribedItem vollständig)
                    ExpressionFunction builtin = reg.get(name); // im EDT für User null, für Builtins OK
                    if (builtin != null) {
                        out.put(name, builtin);
                        continue;
                    }

                    // User-Funktionen: leichtes DescribedItem aus gespeicherten Metadaten
                    final de.bund.zrb.expressions.domain.FunctionMetadata m = reg.getMetadata(name);
                    if (m == null) {
                        out.put(name, new DescribedItem() {
                            public String getDescription() { return ""; }
                            // falls dein DescribedItem diese Methoden nicht hat, kannst du sie weglassen
                            public java.util.List<String> getParamNames() { return java.util.Collections.<String>emptyList(); }
                            public java.util.List<String> getParamDescriptions() { return java.util.Collections.<String>emptyList(); }
                            public Object getMetadata() { return null; }
                        });
                        continue;
                    }

                    out.put(name, new DescribedItem() {
                        public String getDescription() {
                            String d = m.getDescription();
                            return d != null ? d : "";
                        }
                        // >>> WICHTIG: öffentlich, exakte Methodennamen, damit Reflection sie findet
                        public java.util.List<String> getParamNames() {
                            java.util.List<String> n = m.getParameterNames();
                            return n != null ? n : java.util.Collections.<String>emptyList();
                        }
                        public java.util.List<String> getParamDescriptions() {
                            java.util.List<String> d = m.getParameterDescriptions();
                            return d != null ? d : java.util.Collections.<String>emptyList();
                        }
                        public Object getMetadata() {
                            return m; // optional – erlaubt der Reflection einen 2. Pfad
                        }
                    });
                }
                return out;
            }
        };

        // Regex-Presets: aus RegexPatternRegistry (Title- & Message-Presets)
        Supplier<Map<String, DescribedItem>> rxSupplier = new Supplier<Map<String, DescribedItem>>() {
            @Override public Map<String, DescribedItem> get() {
                Map<String, DescribedItem> out = new LinkedHashMap<String, DescribedItem>();
                RegexPatternRegistry rx = RegexPatternRegistry.getInstance();

                java.util.List<String> titles = rx.getTitlePresets();
                for (int i = 0; i < titles.size(); i++) {
                    final String val = titles.get(i);
                    out.put(val, new DescribedItem() {
                        @Override public String getDescription() { return "Regex-Preset (Title)"; }
                    });
                }

                java.util.List<String> msgs = rx.getMessagePresets();
                for (int i = 0; i < msgs.size(); i++) {
                    final String val = msgs.get(i);
                    // bei gleichen Strings überschreibt "Message" Beschreibung — ist ok
                    out.put(val, new DescribedItem() {
                        @Override public String getDescription() { return "Regex-Preset (Message)"; }
                    });
                }
                return out;
            }
        };

        // Attach ExpressionCellEditor to Expression column (index 1)
        ExpressionCellEditor exprEditor = new ExpressionCellEditor(varSupplier, fnSupplier, rxSupplier);
        table.getColumnModel().getColumn(1).setCellEditor(exprEditor);

        // Lightweight 3-line monospaced renderer
        table.getColumnModel().getColumn(1).setCellRenderer(new MultiLineMonoRenderer());

        // Toolbar
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton addBtn = new JButton("+");
        addBtn.setToolTipText(scopeName + " Eintrag hinzufügen");
        addBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                model.addEmptyRow();
            }
        });

        JButton delBtn = new JButton("–");
        delBtn.setToolTipText("Ausgewählte Zeile löschen");
        delBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    model.removeRow(row);
                }
            }
        });

        JButton editBtn = new JButton("Bearbeiten");
        editBtn.setToolTipText("Expression in ausgewählter Zeile bearbeiten");
        editBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) return;
                int exprCol = 1;
                table.editCellAt(row, exprCol);
                Component editorComp = table.getEditorComponent();
                if (editorComp != null) {
                    editorComp.requestFocusInWindow();
                }
            }
        });

        JButton saveBtn = new JButton("💾");
        saveBtn.setToolTipText("Speichern");
        saveBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                TestRegistry.getInstance().save();
                JOptionPane.showMessageDialog(
                        MapTablePanel.this,
                        "Gespeichert.",
                        "Info",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        bar.add(addBtn);
        bar.add(delBtn);
        bar.add(editBtn);
        bar.addSeparator();
        bar.add(saveBtn);

        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /** Lightweight multi-line monospaced renderer to display ~3 lines. */
    static final class MultiLineMonoRenderer extends JTextArea implements TableCellRenderer {
        private final Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);

        MultiLineMonoRenderer() {
            setFont(mono);
            setLineWrap(true);
            setWrapStyleWord(false); // keep code-ish wrapping
            setOpaque(true);
            setRows(3); // hint three lines
            setBorder(null);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : String.valueOf(value));
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

            int fmH = getFontMetrics(getFont()).getHeight();
            int desired = Math.max(table.getRowHeight(), 3 * fmH + 8);
            if (table.getRowHeight() < desired) {
                table.setRowHeight(desired);
            }
            return this;
        }
    }
}
