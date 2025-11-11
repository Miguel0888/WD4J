package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.service.RegexPatternRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.runtime.ExpressionRegistryImpl;
import de.bund.zrb.ui.celleditors.DescribedItem;
import de.bund.zrb.ui.celleditors.ExpressionCellEditor;
import de.bund.zrb.ui.components.RoundIconButton;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public class MapTablePanel extends JPanel {

    /** Behalte die bestehende Signatur – alle bisherigen Aufrufer bleiben kompatibel. */
    public MapTablePanel(final Map<String,String> backing,
                         final Map<String, Boolean> backingEnabled,
                         final String scopeName,
                         final Supplier<List<String>> usersProvider) {
        this(backing, backingEnabled, scopeName, usersProvider, null, null);
    }

    /** Neue Overload nur für „gepinnte“ erste Zeile (z. B. OTP im Root/Templates). */
    public MapTablePanel(final Map<String,String> backing,
                         final Map<String, Boolean> backingEnabled,
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
            if (backingEnabled != null && !backingEnabled.containsKey(pinnedKey)) {
                backingEnabled.put(pinnedKey, Boolean.TRUE);
            }
            needImmediateSave = true;
        }
        if (!includePinnedRow && includeUserRow && backing != null && !backing.containsKey(MapTableModel.USER_KEY)) {
            backing.put(MapTableModel.USER_KEY, "");
            if (backingEnabled != null && !backingEnabled.containsKey(MapTableModel.USER_KEY)) {
                backingEnabled.put(MapTableModel.USER_KEY, Boolean.TRUE);
            }
            needImmediateSave = true;
        }

        final MapTableModel model = new MapTableModel(backing, backingEnabled, includeUserRow, includePinnedRow, pinnedKey);

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
                    if (row == 0 && includeUserRow) {
                        return null; // Name gesperrt
                    }
                    return super.getCellEditor(row, column);
                }
                if (column == 2) {
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
                if (row == 0 && column == 1 && (includeUserRow || includePinnedRow)) {
                    return new UserNameLockedRenderer();
                }
                if (row == 0 && column == 2 && includePinnedRow) {
                    return new ExpressionRenderers.PinnedExpressionRenderer();
                }
                if (column == 2) {
                    return new ExpressionRenderers.ExpressionRenderer();
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
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(80);
            table.getColumnModel().getColumn(0).setMaxWidth(90);
        }
        if (table.getColumnModel().getColumnCount() > 2) {
            table.getColumnModel().getColumn(2).setPreferredWidth(480);
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
        RoundIconButton b = new RoundIconButton("?");
        b.setToolTipText("Hilfe zu Before/Each/Templates anzeigen");
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

    private String buildHelpHtml(String scopeName) {
        StringBuilder sb = new StringBuilder(1600);
        sb.append("<html><body style='font-family:sans-serif; padding:8px;'>");
        sb.append("<h2 style='margin-top:0'>" + scopeName + " – Hilfe</h2>");
        sb.append("<p>In diesem Bereich verwaltest du <strong>Testdaten</strong> und <strong>Expressions</strong> in verschiedenen Auswertungsphasen. Die Struktur ist an Behavior-Driven-Ansätze angelehnt.</p>");

        sb.append("<h3>1. Ebenen & Reihenfolge</h3>");
        sb.append("<table border='0' cellpadding='4' cellspacing='0' style='border-collapse:collapse'>");
        sb.append(row("Root → BeforeAll", "Einmal zu Laufbeginn (global)."));
        sb.append(row("Root → BeforeEach", "Vor <em>jedem</em> TestCase (global frisch)."));
        sb.append(row("Suite → BeforeAll", "Einmal je Suite (suite-spezifisch)."));
        sb.append(row("Suite → BeforeEach", "Vor jedem Case der Suite (suite-frisch)."));
        sb.append(row("Case → Before", "Einmal direkt vor dem Case (case-spezifisch)."));
        sb.append("</table>");
        sb.append("<p style='color:#555'>Shadow-Reihenfolge beim Zugriff: <code>Case → Suite → Root</code>.</p>");

        sb.append("<h3>2. Variablen vs. Templates</h3>");
        sb.append("<ul>");
        sb.append(li("<b>Variablen</b>", "werden zum jeweiligen Before-Zeitpunkt ausgewertet und als String abgelegt. Zugriff in Actions via <code>{{name}}</code>."));
        sb.append(li("<b>Templates</b>", "werden <em>lazy</em> erst bei Nutzung in einer Action berechnet. Ideal für zeit- oder kontextabhängige Werte (OTP, Timestamp, dynamische IDs)."));
        sb.append("</ul>");

        sb.append("<h3>3. Beispiele</h3>");
        sb.append("<pre style='background:#f5f5f5;padding:8px;border:1px solid #ddd;'>" + escape("Root.BeforeAll: baseUrl = https://test/app\nRoot.Templates: OTP = {{otp({{user}})}}\nSuite.BeforeEach: loginToken = {{fetchLoginToken({{user}})}}\nCase.Before: documentId = {{generateDocId()}}") + "</pre>");
        sb.append("<p><b>In einer Action:</b> Value = <code>{{loginToken}}</code> oder Auswahl des Templates <code>OTP</code> → ersetzt automatisch den Ausdruck.</p>");

        sb.append("<h3>4. Gültigkeit & Überschreiben</h3>");
        sb.append("<p>Definierst du einen Namen mehrfach (z. B. einmal in Root.BeforeAll und erneut in Case.Before), gewinnt die <em>nächstliegende</em> Ebene (Case vor Suite vor Root)." );
        sb.append("<h3>5. Best Practices</h3>");
        sb.append("<ul>");
        sb.append(li("Konstanten", "in Root.BeforeAll: Basis-URLs, statische Feature-Flags."));
        sb.append(li("Schnell veraltende Werte", "in Root/Suite BeforeEach: Zeitstempel, Session-Tokens."));
        sb.append(li("Case-spezifische Overrides", "in Case.Before: gezielte Testszenario-Isolation."));
        sb.append(li("Komplexe dynamische Konstruktion", "als Templates (lazy)."));
        sb.append("</ul>");

        sb.append("<h3>6. Fehlerquellen</h3>");
        sb.append("<ul>");
        sb.append(li("Namens-Kollisionen", "führen zu unerwarteten Überschreibungen – eindeutige Präfixe nutzen (z. B. <code>suite_</code>, <code>case_</code>)."));
        sb.append(li("Zu frühe Auswertung", "wenn dynamische Werte fälschlich in BeforeAll stehen – dann Template oder BeforeEach nutzen."));
        sb.append(li("Unnötige Templates", "für einfache Konstanten – lieber direkte Variablen nehmen."));
        sb.append("</ul>");

        sb.append("<h3>7. Laufzeitverhalten</h3>");
        sb.append("<p>Die Engine initialisiert einmal Root.BeforeAll, dann pro Case Root.BeforeEach, Suite.BeforeEach und Case.Before. Templates expandieren bei Nutzung in WHEN/THEN-Actions.</p>");

        sb.append("<p style='margin-top:12px;color:#666'><i>Siehe auch TestPlayerService für die genaue Reihenfolge und Scope-Vererbung.</i></p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    // Hilfsfunktionen für strukturierten HTML-Aufbau
    private String row(String left, String right) {
        return "<tr><td style='padding:2px 6px;font-weight:bold;white-space:nowrap;'>"
                + escape(left) + "</td><td style='padding:2px 6px;'>" + escape(right) + "</td></tr>";
    }
    private String li(String left, String right) {
        return "<li style='margin-bottom:4px'><b>" + escape(left) + ":</b> " + escape(right) + "</li>";
    }
    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
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

}
