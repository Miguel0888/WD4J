package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.service.RegexPatternRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.runtime.ExpressionRegistryImpl;
import de.bund.zrb.ui.celleditors.DescribedItem;
import de.bund.zrb.ui.celleditors.ExpressionCellEditor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public class MapTablePanel extends JPanel {

    /** Behalte die bestehende Signatur – alle bisherigen Aufrufer bleiben kompatibel. */
    public MapTablePanel(final Map<String,String> backing,
                         final String scopeName,
                         final Supplier<List<String>> usersProvider) {
        this(backing, scopeName, usersProvider, null, null);
    }

    /** Neue Overload nur für „gepinnte“ erste Zeile (z. B. OTP im Root/Templates). */
    public MapTablePanel(final Map<String,String> backing,
                         final String scopeName,
                         final Supplier<List<String>> usersProvider,
                         final String pinnedKey,
                         final String pinnedValue) {
        super(new BorderLayout());

        final boolean includeUserRow = (usersProvider != null) && (pinnedKey == null);
        final boolean includePinnedRow = (pinnedKey != null);

        boolean needImmediateSave = false;
        if (includePinnedRow && backing != null && !backing.containsKey(pinnedKey)) {
            backing.put(pinnedKey, pinnedValue != null ? pinnedValue : "");
            needImmediateSave = true;
        }
        if (!includePinnedRow && includeUserRow && backing != null && !backing.containsKey(MapTableModel.USER_KEY)) {
            backing.put(MapTableModel.USER_KEY, "");
            needImmediateSave = true;
        }

        final MapTableModel model = new MapTableModel(backing, includeUserRow, includePinnedRow, pinnedKey);

        final JTable table = new JTable(model) {
            private final TableCellEditor userEditor =
                    (includeUserRow ? buildUserEditor(usersProvider) : null);
            private final ExpressionCellEditor exprEditor = buildExpressionEditor(backing);

            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                if (column == 1) {
                    if (row == 0 && includePinnedRow) {
                        return null;
                    }
                    if (row == 0 && includeUserRow && userEditor != null) {
                        return userEditor;
                    }
                    return exprEditor;
                }
                return super.getCellEditor(row, column);
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (row == 0 && column == 0 && (includeUserRow || includePinnedRow)) {
                    return new UserNameLockedRenderer();
                }
                if (row == 0 && column == 1 && includePinnedRow) {
                    return new PinnedValueLockedRenderer();
                }
                if (column == 1) {
                    return new MultiLineMonoRenderer();
                }
                return super.getCellRenderer(row, column);
            }

            private TableCellEditor buildUserEditor(final Supplier<List<String>> provider) {
                return new UserComboBoxCellEditor(new UserComboBoxCellEditor.ChoicesProvider() {
                    public List<String> getUsers() {
                        return provider != null ? provider.get() : java.util.Collections.<String>emptyList();
                    }
                });
            }

            private ExpressionCellEditor buildExpressionEditor(final Map<String,String> backingMap) {
                return new ExpressionCellEditor(
                        buildVarSupplier(backingMap),
                        buildFnSupplier(),
                        buildRxSupplier()
                );
            }
        };

        // UX (unverändert)
        table.setFillsViewportHeight(true);
        table.setSurrendersFocusOnKeystroke(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        int fmH = table.getFontMetrics(table.getFont()).getHeight();
        table.setRowHeight(Math.max(table.getRowHeight(), 3 * fmH + 8));
        if (table.getColumnModel().getColumnCount() > 1) {
            table.getColumnModel().getColumn(1).setPreferredWidth(480);
        }

        // Toolbar (neu via Builder, Inhalt identisch, Hilfe/Save nun rechtsbündig)
        JToolBar bar = buildToolbar(scopeName, model, table, includePinnedRow, pinnedKey);
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        if (needImmediateSave) {
            try { TestRegistry.getInstance().save(); } catch (Throwable ignore) { }
        }
    }

    // Build toolbar with left-aligned CRUD + save and right-aligned help
    private JToolBar buildToolbar(final String scopeName,
                                  final MapTableModel model,
                                  final JTable table,
                                  final boolean includePinnedRow,
                                  final String pinnedKey) {
        // Use BoxLayout to allow glue-based right alignment
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setLayout(new BoxLayout(bar, BoxLayout.X_AXIS)); // force BoxLayout semantics

        JButton addBtn  = buildAddButton(model, scopeName);
        JButton delBtn  = buildDeleteButton(model, table, includePinnedRow, pinnedKey);
        JButton editBtn = buildEditButton(table);
        JButton saveBtn = buildSaveButton();          // stays on the left
        JButton helpBtn = buildHelpButton(scopeName); // goes to the far right

        // Left group (unchanged order, save stays left)
        bar.add(addBtn);
        bar.add(delBtn);
        bar.add(editBtn);
        bar.addSeparator();
        bar.add(saveBtn);

        // Glue pushes following components (only help) to the far right
        bar.add(Box.createHorizontalGlue());

        // Right group
        bar.add(helpBtn);

        return bar;
    }

    // Create "+" button
    private JButton buildAddButton(final MapTableModel model, final String scopeName) {
        JButton addBtn = new JButton("+");
        addBtn.setToolTipText(scopeName + " Eintrag hinzufügen");
        addBtn.addActionListener(e -> model.addEmptyRow());
        return addBtn;
    }

    // Create "–" button with pinned/user protection identical to original behavior
    private JButton buildDeleteButton(final MapTableModel model,
                                      final JTable table,
                                      final boolean includePinnedRow,
                                      final String pinnedKey) {
        JButton delBtn = new JButton("–");
        delBtn.setToolTipText("Ausgewählte Zeile löschen");
        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                if (!model.canRemoveRow(row)) {
                    String pinnedName = includePinnedRow ? pinnedKey : MapTableModel.USER_KEY;
                    JOptionPane.showMessageDialog(
                            MapTablePanel.this,
                            "\"" + pinnedName + "\" kann nicht gelöscht werden.",
                            "Hinweis",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    return;
                }
                model.removeRow(row);
            }
        });
        return delBtn;
    }

    // Create "Bearbeiten" button
    private JButton buildEditButton(final JTable table) {
        JButton editBtn = new JButton("Bearbeiten");
        editBtn.setToolTipText("Expression in ausgewählter Zeile bearbeiten");
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            table.editCellAt(row, 1);
            Component editorComp = table.getEditorComponent();
            if (editorComp != null) editorComp.requestFocusInWindow();
        });
        return editBtn;
    }

    // Create "💾" button (left side)
    private JButton buildSaveButton() {
        JButton saveBtn = new JButton("💾");
        saveBtn.setToolTipText("Speichern");
        saveBtn.addActionListener(e -> {
            // Persist changes immediately
            TestRegistry.getInstance().save();
        });
        return saveBtn;
    }

    // ---- Hilfe-Button ----------------------------------------------------------

    /** Erzeuge einen blauen Hilfe-Button mit verständlicher Erläuterung (Cucumber-artig). */
    private JButton buildHelpButton(final String scopeName) {
        JButton b = new JButton("ℹ");
        b.setToolTipText("Was bedeuten „Before…“ und „Templates“?");
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(0x1E88E5)); // Blau
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x1565C0)),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));

        b.addActionListener(e -> {
            String html = buildHelpHtml(scopeName);
            JOptionPane.showMessageDialog(
                    MapTablePanel.this,
                    new JScrollPane(wrapAsHtmlPane(html)),
                    "Hilfe zu \"" + scopeName + "\"",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
        return b;
    }

    /** Baue den erklärenden Text abhängig vom Scope-Namen. */
    private String buildHelpHtml(String scopeName) {
        String scope = (scopeName == null) ? "" : scopeName.trim().toLowerCase();
        boolean isRoot   = "beforeall".equalsIgnoreCase(scope) || "beforeeach".equalsIgnoreCase(scope) || "templates".equalsIgnoreCase(scope);
        // Wir zeigen für alle Scopes die vollständige Erklärung; die Beispiele nennen Root/Suite/Case explizit.

        StringBuilder sb = new StringBuilder(1024);
        sb.append("<html><body style='font-family:sans-serif; padding:8px;'>");
        sb.append("<h3 style='margin-top:0'>Wie lese ich diese Tabellen?</h3>");

        sb.append("<p><b>Idee (angelehnt an Cucumber):</b> ")
          .append("Hier verwaltest du Testdaten und Bausteine in drei Ebenen – <i>Root</i>, <i>Suite</i> und <i>Case</i>. ")
          .append("Die Werte werden je nach Tab zu unterschiedlichen Zeitpunkten berechnet (\"evaluiert\").</p>");

        sb.append("<h4>Tabs & Auswertungszeitpunkte</h4>");
        sb.append("<ul>");
        sb.append("<li><b>Root → BeforeAll:</b> wird genau <u>einmal beim ersten Suite-Start</u> evaluiert. ")
          .append("Typisch für dauerhafte Dinge wie Basis-URLs, Mandanten, Feature-Flags.</li>");
        sb.append("<li><b>Root → BeforeEach:</b> wird <u>vor jedem TestCase</u> evaluiert. ")
          .append("Gut für Werte, die pro Testlauf frisch sein sollen (z. B. Datum, zufällige IDs).</li>");
        sb.append("<li><b>Suite → BeforeAll:</b> wird <u>einmal pro Suite</u> evaluiert. ")
          .append("Nutze es für suite-spezifische Konstanten.</li>");
        sb.append("<li><b>Suite → BeforeEach:</b> wird <u>vor jedem Case der Suite</u> evaluiert. ")
          .append("Hier z. B. Logins oder vorbereitende Daten für die Suite.</li>");
        sb.append("<li><b>Case → Before:</b> existiert nur auf Case-Ebene und wird <u>einmal zu Beginn des Cases</u> evaluiert. ")
          .append("Damit überschreibst oder ergänzt du Werte gezielt für diesen einen Case.</li>");
        sb.append("</ul>");

        sb.append("<h4>Templates (lazy)</h4>");
        sb.append("<p><b>Templates</b> sind wie Funktionsbausteine: Sie werden <i>nicht sofort</i> berechnet, ")
          .append("sondern erst <u>im Moment der Verwendung</u> in einer Action (lazy). ")
          .append("So kannst du zeitkritische Dinge genau dann erzeugen, wenn sie gebraucht werden.</p>");
        sb.append("<p><b>Beispiel:</b> Das Template <code>OTP</code> könnte so definiert sein: ")
          .append("<code>{{OTP({{user}})}}</code>. ")
          .append("Wenn eine Action das Template nutzt (z. B. beim Button-Klick), wird der OTP-Code ")
          .append("erst dann mit dem <i>aktuellen</i> User berechnet.</p>");

        sb.append("<h4>Variablen vs. Templates</h4>");
        sb.append("<ul>");
        sb.append("<li><b>Variablen</b> (in <i>Before…</i> Tabellen) werden zum jeweiligen Zeitpunkt evaluiert und als fester String abgelegt. ")
          .append("In Actions greifst du mit <code>{{variablenName}}</code> darauf zu.</li>");
        sb.append("<li><b>Templates</b> bleiben Ausdrücke und werden bei Benutzung aufgelöst. ")
          .append("In Actions wählst du das Template im Dropdown, und der vollständige Ausdruck wird in das Value-Feld übernommen.</li>");
        sb.append("</ul>");

        sb.append("<h4>Auflösung in der Laufzeit</h4>");
        sb.append("<p>Die Laufzeit löst Werte in folgender Reihenfolge auf (Schatten-Prinzip): ")
          .append("<i>Case → Suite → Root</i>. ")
          .append("So kann ein Case einen Wert aus Suite/Root überschreiben.</p>");

        sb.append("<p style='margin-top:10px;color:#555'><i>Hinweis:</i> Details zur Auswertung findest du im Player (")
          .append("<code>TestPlayerService</code>), dort wird z. B. <i>Root.BeforeAll</i> nur ein einziges Mal initialisiert, ")
          .append("während <i>BeforeEach</i> je TestCase frisch berechnet wird.</p>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private JEditorPane wrapAsHtmlPane(String html) {
        JEditorPane pane = new JEditorPane("text/html", html);
        pane.setEditable(false);
        pane.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        pane.setCaretPosition(0);
        return pane;
    }

    // ---- Renderer/Editor helpers ----

    static final class UserNameLockedRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setForeground(isSelected ? table.getSelectionForeground() : Color.GRAY);
            c.setFont(c.getFont().deriveFont(Font.ITALIC));
            return c;
        }
    }

    static final class UserComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor {
        interface ChoicesProvider { List<String> getUsers(); }
        private final JComboBox<String> combo = new JComboBox<String>();
        private final ChoicesProvider provider;

        UserComboBoxCellEditor(ChoicesProvider provider) {
            this.provider = provider;
            combo.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
            combo.setEditable(false);
            rebuild();
        }

        private void rebuild() {
            combo.removeAllItems();
            combo.addItem(""); // leerer Eintrag
            List<String> users = provider != null ? provider.getUsers() : null;
            if (users != null) {
                for (int i = 0; i < users.size(); i++) {
                    String u = users.get(i);
                    if (u != null) {
                        String t = u.trim();
                        if (t.length() > 0) combo.addItem(t);
                    }
                }
            }
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (combo.getItemCount() == 0) rebuild();
            combo.setSelectedItem(value == null ? "" : String.valueOf(value));
            return combo;
        }

        public Object getCellEditorValue() {
            Object sel = combo.getSelectedItem();
            return sel == null ? "" : String.valueOf(sel);
        }
    }

    static final class MultiLineMonoRenderer extends JTextArea implements TableCellRenderer {
        private final Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        MultiLineMonoRenderer() {
            setFont(mono);
            setLineWrap(true);
            setWrapStyleWord(false);
            setOpaque(true);
            setRows(3);
            setBorder(null);
        }
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

    // ---- Suppliers (unverändert, nur ausgelagert, damit oben kurz bleibt) ----

    private Supplier<List<String>> buildVarSupplier(final Map<String,String> backing) {
        return new Supplier<List<String>>() {
            @Override public List<String> get() {
                Set<String> all = new LinkedHashSet<String>();
                if (backing != null) all.addAll(backing.keySet());

                de.bund.zrb.model.RootNode root = TestRegistry.getInstance().getRoot();
                if (root != null) {
                    if (root.getBeforeAll()   != null) all.addAll(root.getBeforeAll().keySet());
                    if (root.getBeforeEach()  != null) all.addAll(root.getBeforeEach().keySet());
                    if (root.getTemplates()   != null) all.addAll(root.getTemplates().keySet());
                }
                List<de.bund.zrb.model.TestSuite> suites = TestRegistry.getInstance().getAll();
                if (suites != null) {
                    for (int si = 0; si < suites.size(); si++) {
                        de.bund.zrb.model.TestSuite s = suites.get(si);
                        if (s.getBeforeAll()   != null) all.addAll(s.getBeforeAll().keySet());
                        if (s.getBeforeEach()  != null) all.addAll(s.getBeforeEach().keySet());
                        if (s.getTemplates()   != null) all.addAll(s.getTemplates().keySet());
                        List<de.bund.zrb.model.TestCase> cases = s.getTestCases();
                        if (cases != null) {
                            for (int ci = 0; ci < cases.size(); ci++) {
                                de.bund.zrb.model.TestCase tc = cases.get(ci);
                                if (tc.getBefore()    != null) all.addAll(tc.getBefore().keySet());
                                if (tc.getTemplates() != null) all.addAll(tc.getTemplates().keySet());
                            }
                        }
                    }
                }
                List<String> sorted = new ArrayList<String>(all);
                Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
                return sorted;
            }
        };
    }

    private Supplier<Map<String, DescribedItem>> buildFnSupplier() {
        return new Supplier<Map<String, DescribedItem>>() {
            @Override public Map<String, DescribedItem> get() {
                Map<String, DescribedItem> out = new LinkedHashMap<String, DescribedItem>();
                ExpressionRegistryImpl reg = ExpressionRegistryImpl.getInstance();
                Set<String> keys = reg.getKeys();
                List<String> sorted = new ArrayList<String>(keys);
                Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
                for (int i = 0; i < sorted.size(); i++) {
                    final String name = sorted.get(i);
                    ExpressionFunction builtin = reg.get(name);
                    if (builtin != null) { out.put(name, builtin); continue; }
                    final de.bund.zrb.expressions.domain.FunctionMetadata m = reg.getMetadata(name);
                    out.put(name, new DescribedItem() {
                        public String getDescription() {
                            return m != null && m.getDescription() != null ? m.getDescription() : "";
                        }
                        public java.util.List<String> getParamNames() {
                            return m != null && m.getParameterNames() != null ? m.getParameterNames()
                                    : java.util.Collections.<String>emptyList();
                        }
                        public java.util.List<String> getParamDescriptions() {
                            return m != null && m.getParameterDescriptions() != null ? m.getParameterDescriptions()
                                    : java.util.Collections.<String>emptyList();
                        }
                        public Object getMetadata() { return m; }
                    });
                }
                return out;
            }
        };
    }

    private Supplier<Map<String, DescribedItem>> buildRxSupplier() {
        return new Supplier<Map<String, DescribedItem>>() {
            @Override public Map<String, DescribedItem> get() {
                Map<String, DescribedItem> out = new LinkedHashMap<String, DescribedItem>();
                RegexPatternRegistry rx = RegexPatternRegistry.getInstance();
                List<String> titles = rx.getTitlePresets();
                for (int i = 0; i < titles.size(); i++) {
                    final String val = titles.get(i);
                    out.put(val, new DescribedItem() {
                        @Override public String getDescription() { return "Regex-Preset (Title)"; }
                    });
                }
                List<String> msgs = rx.getMessagePresets();
                for (int i = 0; i < msgs.size(); i++) {
                    final String val = msgs.get(i);
                    out.put(val, new DescribedItem() {
                        @Override public String getDescription() { return "Regex-Preset (Message)"; }
                    });
                }
                return out;
            }
        };
    }

    /** Gray, italic, locked look for the pinned value cell (row 0, col 1). */
    static final class PinnedValueLockedRenderer extends JTextArea implements TableCellRenderer {
        private final Font mono = new Font(Font.MONOSPACED, Font.ITALIC, 12);

        PinnedValueLockedRenderer() {
            setFont(mono);
            setLineWrap(true);
            setWrapStyleWord(false);
            setOpaque(true);
            setRows(3);
            setBorder(null);
            setEditable(false);
            setEnabled(false);
        }

        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            setText(value == null ? "" : String.valueOf(value));
            // dezent ausgegraut; bei Selektion gut lesbar lassen
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(Color.GRAY);
                setBackground(table.getBackground());
            }

            int fmH = getFontMetrics(getFont()).getHeight();
            int desired = Math.max(table.getRowHeight(), 3 * fmH + 8);
            if (table.getRowHeight() < desired) {
                table.setRowHeight(desired);
            }
            return this;
        }
    }

}
