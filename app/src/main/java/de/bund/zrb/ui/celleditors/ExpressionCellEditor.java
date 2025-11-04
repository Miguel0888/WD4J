// src/main/java/de/bund/zrb/ui/celleditors/ExpressionCellEditor.java
package de.bund.zrb.ui.celleditors;

import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

import static org.fife.ui.autocomplete.Util.startsWithIgnoreCase;

public class ExpressionCellEditor extends javax.swing.AbstractCellEditor implements TableCellEditor {

    private final RSyntaxTextArea textArea = new RSyntaxTextArea(6, 40);
    private final AutoCompletion autoCompletion;
    private final AlwaysOnProvider provider;

    private final Supplier<List<String>> variableNamesSupplier;
    private final Supplier<Map<String, DescribedItem>> functionItemsSupplier;
    private final Supplier<Map<String, DescribedItem>> regexItemsSupplier;

    public ExpressionCellEditor(Supplier<List<String>> variableNamesSupplier,
                                Supplier<Map<String, DescribedItem>> functionItemsSupplier,
                                Supplier<Map<String, DescribedItem>> regexItemsSupplier) {

        this.variableNamesSupplier = variableNamesSupplier != null
                ? variableNamesSupplier
                : new Supplier<List<String>>() { @Override public List<String> get() { return java.util.Collections.<String>emptyList(); } };

        this.functionItemsSupplier = functionItemsSupplier != null
                ? functionItemsSupplier
                : new Supplier<Map<String, DescribedItem>>() { @Override public Map<String, DescribedItem> get() { return java.util.Collections.<String, DescribedItem>emptyMap(); } };

        this.regexItemsSupplier = regexItemsSupplier != null
                ? regexItemsSupplier
                : new Supplier<Map<String, DescribedItem>>() { @Override public Map<String, DescribedItem> get() { return java.util.Collections.<String, DescribedItem>emptyMap(); } };

        configureEditorArea(textArea);

        provider = new AlwaysOnProvider(this.variableNamesSupplier, this.functionItemsSupplier, this.regexItemsSupplier);
        provider.setAutoActivationRules(true, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_({[\"';*}]) ");

        autoCompletion = new TabbedAutoCompletion(provider, "Functions", "Variables", "Regex");
        autoCompletion.setAutoActivationEnabled(true);
        autoCompletion.setAutoActivationDelay(80);
        autoCompletion.setParameterAssistanceEnabled(false); // avoid library adding ()
        autoCompletion.setTriggerKey(KeyStroke.getKeyStroke("control SPACE"));
        autoCompletion.setAutoCompleteSingleChoices(false);
        autoCompletion.setShowDescWindow(true);
        autoCompletion.install(textArea);

        installAlwaysShowPopupHooks(textArea, autoCompletion);
        installEnterBehavior(textArea);
    }

    private static void configureEditorArea(RSyntaxTextArea ta) {
        ta.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        ta.setCodeFoldingEnabled(false);
        ta.setBracketMatchingEnabled(true);
        ta.setAnimateBracketMatching(true);
        ta.setAutoIndentEnabled(true);
        ta.setTabsEmulated(true);
        ta.setTabSize(2);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(false);

        ta.setRows(3);
        int lineH = ta.getFontMetrics(ta.getFont()).getHeight();
        ta.setPreferredSize(new Dimension(0, 3 * lineH + 8));
    }

    private static void installEnterBehavior(final RSyntaxTextArea ta) {
        final InputMap im = ta.getInputMap();
        final ActionMap am = ta.getActionMap();
        im.put(KeyStroke.getKeyStroke("ENTER"), "commit-or-newline");
        am.put("commit-or-newline", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
                int mods = e != null ? e.getModifiers() : 0;
                boolean ctrlOrCmd = (mods & menuMask) != 0;
                boolean shift = (mods & java.awt.event.InputEvent.SHIFT_MASK) != 0;
                if (ctrlOrCmd || shift) {
                    ta.replaceSelection("\n");
                } else {
                    ta.transferFocus();
                }
            }
        });
    }

    private static void installAlwaysShowPopupHooks(final RSyntaxTextArea ta, final AutoCompletion ac) {
        ta.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { ac.doCompletion(); }
        });
        ta.addCaretListener(new javax.swing.event.CaretListener() {
            @Override public void caretUpdate(javax.swing.event.CaretEvent e) { ac.doCompletion(); }
        });
        ta.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_ESCAPE) ac.doCompletion();
            }
        });
    }

    // ---------- Provider ----------

    private static final class AlwaysOnProvider extends DefaultCompletionProvider {

        private final Supplier<List<String>> variableNamesSupplier;
        private final Supplier<Map<String, DescribedItem>> functionItemsSupplier;
        private final Supplier<Map<String, DescribedItem>> regexItemsSupplier;

        AlwaysOnProvider(Supplier<List<String>> variableNamesSupplier,
                         Supplier<Map<String, DescribedItem>> functionItemsSupplier,
                         Supplier<Map<String, DescribedItem>> regexItemsSupplier) {
            this.variableNamesSupplier = variableNamesSupplier;
            this.functionItemsSupplier = functionItemsSupplier;
            this.regexItemsSupplier = regexItemsSupplier;
        }

        @Override public char   getParameterListStart()       { return '\0'; } // do not bind '('
        @Override public String getParameterListSeparator()   { return "; "; }
        @Override public char   getParameterListEnd()         { return ')'; }

        @Override protected boolean isValidChar(char ch) {
            return Character.isLetterOrDigit(ch) || ch == '_' || ch == '*' || ch == '"' || ch == '{' || ch == '}';
        }

        @Override public boolean isAutoActivateOkay(JTextComponent tc) { return true; }
//
//
//        private void addFunctionCompletions(List out, String prefix) {
//            Map<String, DescribedItem> map = functionItemsSupplier.get();
//            List<String> names = sortedKeys(map.keySet());
//            for (int i = 0; i < names.size(); i++) {
//                String fn = names.get(i);
//                if (!startsWithIgnoreCase(fn, prefix)) continue;
//
//                DescribedItem di = map.get(fn);
//                String desc = di != null ? di.getDescription() : null;
//
//                // Parameternamen holen – aus deiner Quelle füllen:
//                // z. B. di.getParamNames() falls vorhanden; sonst leer
//                java.util.List<String> paramNames = di != null ? di.getParamNames() : java.util.Collections.<String>emptyList();
//                java.util.List<String> paramDescs = di != null ? di.getParamDescriptions() : null;
//
//                out.add(new FunctionCompletionWrapped(this, fn, desc, paramNames, paramDescs));
//            }
//        }


        @Override
        public String getAlreadyEnteredText(JTextComponent comp) {
            try {
                int caret = comp.getCaretPosition();
                if (caret <= 0) return "";
                String text = comp.getDocument().getText(0, caret);
                int i = text.length() - 1;
                StringBuilder sb = new StringBuilder();
                while (i >= 0) {
                    char ch = text.charAt(i);
                    if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '*') {
                        sb.insert(0, ch);
                        i--;
                    } else break;
                }
                return sb.toString();
            } catch (BadLocationException e) {
                return "";
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public List getCompletions(JTextComponent comp) {
            List out = new ArrayList();

            String prefix = getAlreadyEnteredText(comp);

            List<String> vars = variableNamesSupplier.get();
            for (int i = 0; i < vars.size(); i++) {
                String v = vars.get(i);
                if (prefix.length() == 0 || startsWithIgnoreCase(v, prefix)) {
                    out.add(new VariableCompletion(this, v));
                }
            }

            Map<String, DescribedItem> fmap = functionItemsSupplier.get();
            List<String> fnNames = sortedKeys(fmap.keySet());
            for (int i = 0; i < fnNames.size(); i++) {
                String fn = fnNames.get(i);
                if (prefix.length() == 0 || startsWithIgnoreCase(fn, prefix)) {
                    String desc = fmap.containsKey(fn) ? fmap.get(fn).getDescription() : null;
                    out.add(new FunctionCompletionWrapped(this, fn, desc));
                }
            }

            Map<String, DescribedItem> rxmap = regexItemsSupplier.get();
            List<String> rxs = sortedKeys(rxmap.keySet());
            for (int i = 0; i < rxs.size(); i++) {
                String rx = rxs.get(i);
                if (prefix.length() == 0 || startsWithIgnoreCase(rx, prefix)) {
                    String desc = rxmap.containsKey(rx) ? rxmap.get(rx).getDescription() : null;
                    out.add(new RegexCompletion(this, rx, desc));
                }
            }

            return out;
        }

        private List<String> sortedKeys(java.util.Set<String> keys) {
            List<String> list = new ArrayList<String>(keys);
            Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
            return list;
        }
    }

    // ---------- Completion-Typen ----------

    private static final class VariableCompletion extends BasicCompletion {
        private final String name;
        VariableCompletion(CompletionProvider provider, String variableName) {
            super(provider, variableName);
            this.name = variableName;
        }
        @Override public String getInputText() { return escapeHtml(name); }
        @Override public String getReplacementText() { return "{{" + name + "}}"; }
        String getVariableName() { return name; }
        private String escapeHtml(String s) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '&': sb.append("&amp;"); break;
                    case '<': sb.append("&lt;"); break;
                    case '>': sb.append("&gt;"); break;
                    case '"': sb.append("&quot;"); break;
                    case '\'': sb.append("&#39;"); break;
                    default: sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    // --- ersetzt die bisherige FunctionCompletionWrapped ---
    private static final class FunctionCompletionWrapped extends FunctionCompletion {

        private final String fn;
        private final String longDescription; // frei formulierter Beschreibungstext (aus Registry/Catalog)
        private final java.util.List<ParamInfo> paramInfos; // Namen + optionale Kurzbeschreibung

        /** Hilfs-DTO nur für Namen/Beschreibung der Parameter (typfrei, da wir keine echte Typen brauchen). */
        private static final class ParamInfo {
            final String name; final String desc;
            ParamInfo(String name, String desc) { this.name = name; this.desc = desc; }
        }

        /**
         * @param provider CompletionProvider
         * @param functionName Klarname der Funktion (z. B. "Navigate")
         * @param description  Längere Beschreibung, wird im Description-Pane gezeigt (HTML erlaubt)
         * @param paramNames   Reihenfolge der Parameter (Anzeige im Pane & für Summary)
         * @param paramDescs   Optionale Kurzbeschreibungen je Parameter; kann null oder kürzer sein
         */
        FunctionCompletionWrapped(CompletionProvider provider,
                                  String functionName,
                                  String description,
                                  java.util.List<String> paramNames,
                                  java.util.List<String> paramDescs) {

            super(provider, functionName, null);
            this.fn = functionName;
            this.longDescription = description != null ? description : "";

            // 1) baue Parameterliste für FunctionCompletion (damit getParamCount/getParam(i) gefüllt sind)
            java.util.List<ParameterizedCompletion.Parameter> pcParams = new java.util.ArrayList<ParameterizedCompletion.Parameter>();
            java.util.List<ParamInfo> infos = new java.util.ArrayList<ParamInfo>();

            if (paramNames != null) {
                for (int i = 0; i < paramNames.size(); i++) {
                    String pname = paramNames.get(i);
                    String pdesc = (paramDescs != null && i < paramDescs.size()) ? paramDescs.get(i) : null;

                    // Library-Parameter: (type, name, description). Typ brauchen wir nicht -> null.
                    ParameterizedCompletion.Parameter p =
                            new ParameterizedCompletion.Parameter(null, pname);
                    p.setDescription(pdesc);
                    pcParams.add(p);

                    infos.add(new ParamInfo(pname, pdesc));
                }
            }

            setParams(pcParams);                // <- wichtig: damit die Basisklasse Param-Infos hat
            this.paramInfos = infos;

            // Optional: Rückgabebeschreibung, wenn du willst
            // setReturnValueDescription("String");

            // Kurze Beschreibung (List-Tooltip), falls Popup einen kurzen Text verwendet
            if (description != null && description.length() > 0) {
                setShortDescription(description);
            }
        }

        // Anzeige in der Liste
        @Override public String getInputText() {
            return "<html><b>" + escapeHtml(fn) + "</b></html>";
        }

        // Text, der ins Dokument eingefügt wird
        @Override public String getReplacementText() { return "{{" + fn + "()}}"; }

        String getFunctionName() { return fn; }

        // Reichhaltige HTML-Description rechts – nutzt Param-Infos
        @Override
        public String getSummary() {
            StringBuilder sb = new StringBuilder(512);
            sb.append("<html><body style='font-family:sans-serif;font-size:12px;'>");

            // Kopf
            sb.append("<div style='font-weight:bold;font-size:13px;margin-bottom:6px;'>")
                    .append(escapeHtml(fn)).append("</div>");

            // Beschreibung
            if (longDescription != null && longDescription.length() > 0) {
                sb.append("<div style='margin-bottom:8px;'>").append(longDescription).append("</div>");
            }

            // Parameter-Tabelle (benutze Namen/Beschreibung)
            if (paramInfos != null && !paramInfos.isEmpty()) {
                sb.append("<div style='margin-top:4px;'><b>Parameters</b></div>");
                sb.append("<table width='100%' cellspacing='0' cellpadding='2' style='border-collapse:collapse;'>");
                for (int i = 0; i < paramInfos.size(); i++) {
                    ParamInfo p = paramInfos.get(i);
                    sb.append("<tr>")
                            .append("<td valign='top' style='white-space:nowrap;'><code>")
                            .append(escapeHtml(p.name))
                            .append("</code></td>")
                            .append("<td valign='top' style='padding-left:6px;'>")
                            .append(p.desc != null ? escapeHtml(p.desc) : "")
                            .append("</td>")
                            .append("</tr>");
                }
                sb.append("</table>");
            }

            sb.append("</body></html>");
            return sb.toString();
        }

        private String escapeHtml(String s) {
            if (s == null) return "";
            StringBuilder out = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '&': out.append("&amp;"); break;
                    case '<': out.append("&lt;"); break;
                    case '>': out.append("&gt;"); break;
                    case '"': out.append("&quot;"); break;
                    case '\'': out.append("&#39;"); break;
                    default: out.append(c);
                }
            }
            return out.toString();
        }
    }


    private static final class RegexCompletion extends BasicCompletion {
        private final String rx;
        RegexCompletion(CompletionProvider provider, String regexName, String description) {
            super(provider, regexName, description);
            this.rx = regexName;
        }
        @Override public String getInputText() { return "<html><i>" + escapeHtml(rx) + "</i></html>"; }
        @Override public String getReplacementText() { return "\"" + rx + "\""; }
        String getPattern() { return rx; }
        private String escapeHtml(String s) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '&': sb.append("&amp;"); break;
                    case '<': sb.append("&lt;"); break;
                    case '>': sb.append("&gt;"); break;
                    case '"': sb.append("&quot;"); break;
                    case '\'': sb.append("&#39;"); break;
                    default: sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    // ---------- AutoCompletion mit Kontext- und Last-Arg-Erkennung ----------

    private static class CursorAutoCompletion extends AutoCompletion {

        CursorAutoCompletion(CompletionProvider provider) { super(provider); }

        @Override
        protected void insertCompletion(Completion c, boolean typedParamListStartChar) {
            JTextComponent tc = getTextComponent();
            String alreadyEntered = c.getAlreadyEntered(tc);

            hideChildWindows();

            javax.swing.text.Caret caret = tc.getCaret();
            int dot = caret.getDot();
            int len = alreadyEntered != null ? alreadyEntered.length() : 0;
            int start = dot - len;

            // Entferne evtl. bereits getippten Prefix
            caret.setDot(start);
            caret.moveDot(dot);
            tc.replaceSelection("");
            int pos = tc.getCaretPosition();

            // Kontext: Variable/Regex/Funktion innerhalb von {{fn( … )}} ?
            if ((c instanceof VariableCompletion) || (c instanceof RegexCompletion) || (c instanceof FunctionCompletionWrapped)) {
                Bounds b = findFnBoundsAt(tc.getDocument(), pos);
                if (b != null && pos >= b.parenOpen + 1 && pos <= b.parenClose) {

                    // Bestimme, ob erster oder weiterer Parameter (nur Links-Scan)
                    boolean firstParam = isFirstParamPosition(b, pos, tc.getDocument());

                    StringBuilder sb = new StringBuilder();
                    if (!firstParam) {
                        sb.append("; ");
                    }

                    // Merke Einfüge-Start, damit wir den Caret nach dem Replace korrekt setzen
                    int insertStart = tc.getCaretPosition();

                    if (c instanceof VariableCompletion) {
                        VariableCompletion vc = (VariableCompletion) c;
                        sb.append("\"{{").append(vc.getVariableName()).append("}}\"");
                        tc.replaceSelection(sb.toString());
                        // Caret bleibt hinter dem eingefügten Token
                        return;
                    } else if (c instanceof RegexCompletion) {
                        sb.append(((RegexCompletion) c).getReplacementText()); // already quoted
                        tc.replaceSelection(sb.toString());
                        // Caret bleibt hinter dem eingefügten Token
                        return;
                    } else {
                        // --- NEW: Funktion als Argument einfügen: {{name()}}
                        FunctionCompletionWrapped fcw = (FunctionCompletionWrapped) c;
                        String fn = fcw.getFunctionName();
                        sb.append("{{").append(fn).append("()}}");
                        tc.replaceSelection(sb.toString());

                        // Setze Caret in die inneren Klammern der soeben eingefügten Funktion
                        // prefixLen = 2 wenn "; " eingefügt wurde, sonst 0
                        int prefixLen = firstParam ? 0 : 2;
                        int caretPosInInserted = insertStart + prefixLen + 2 /*{{*/ + fn.length() + 1 /*(*/;
                        tc.getCaret().setDot(caretPosInInserted);
                        return;
                    }
                }
            }

            // Standardpfad: Funktionen und Variablen außerhalb der Argumentliste
            Document doc = tc.getDocument();
            String repl = c.getReplacementText();
            tc.replaceSelection(repl);

            if (c instanceof FunctionCompletionWrapped) {
                FunctionCompletionWrapped fc = (FunctionCompletionWrapped) c;
                // Cursor nach '(' in {{fn(|)}}
                int caretPos = start + 2 + fc.getFunctionName().length() + 1;
                caret.setDot(caretPos);
            }
        }

        // ---- Helfer ------------------------------------------------------------

        static final class Bounds {
            String text;
            int blockOpen;
            int blockClose;
            int parenOpen;
            int parenClose;
        }

        /** Finde den umgebenden {{fn(...)}}-Block für 'pos' per balanciertem Scan. */
        Bounds findFnBoundsAt(Document doc, int pos) {
            try {
                String s = doc.getText(0, doc.getLength());
                final int N = s.length();

                // --- 1) Passendes öffnendes "{{" LINKS von pos finden (balanciert) ---
                int openIdx = -1;
                int count = 0; // Anzahl offener "}}" die wir noch "ausgleichen" müssen
                int i = Math.min(Math.max(pos - 1, 0), N - 1);

                while (i >= 0) {
                    // prüfe auf "}}"
                    if (i - 1 >= 0 && s.charAt(i - 1) == '}' && s.charAt(i) == '}') {
                        count++;           // ein Close gesehen
                        i -= 2;
                        continue;
                    }
                    // prüfe auf "{{"
                    if (i - 1 >= 0 && s.charAt(i - 1) == '{' && s.charAt(i) == '{') {
                        if (count == 0) {  // dies ist die öffnende Klammer unseres Blocks
                            openIdx = i - 1;
                            break;
                        }
                        count--;           // ein Close ausgleichen
                        i -= 2;
                        continue;
                    }
                    i--;
                }
                if (openIdx < 0) return null;

                // --- 2) Zugehöriges schließendes "}}" RECHTS via Depth zählen ---
                int depth = 1; // wir stehen direkt hinter "{{"
                int closeIdx = -1;
                i = openIdx + 2;
                while (i < N - 1) {
                    // "{{"?
                    if (s.charAt(i) == '{' && s.charAt(i + 1) == '{') {
                        depth++;
                        i += 2;
                        continue;
                    }
                    // "}}"?
                    if (s.charAt(i) == '}' && s.charAt(i + 1) == '}') {
                        depth--;
                        if (depth == 0) {
                            closeIdx = i;
                            break;
                        }
                        i += 2;
                        continue;
                    }
                    i++;
                }
                if (closeIdx < 0) return null;

                // --- 3) Parameterklammern innerhalb des gefundenen Blocks ---
                int parenOpen  = s.indexOf('(', openIdx + 2);
                if (parenOpen  < 0 || parenOpen  >= closeIdx) return null;
                int parenClose = s.indexOf(')', parenOpen + 1);
                if (parenClose < 0 || parenClose >= closeIdx) return null;

                Bounds b = new Bounds();
                b.text       = s;
                b.blockOpen  = openIdx;
                b.blockClose = closeIdx;
                b.parenOpen  = parenOpen;
                b.parenClose = parenClose;
                return b;

            } catch (BadLocationException e) {
                return null;
            }
        }

        /** True, wenn zwischen '(' und pos nur Whitespace liegt → erster Parameter. */
        private boolean isFirstParamPosition(Bounds b, int pos, Document doc) {
            int i = pos - 1;
            int min = b.parenOpen + 1;
            try {
                while (i >= min) {
                    char ch = doc.getText(i, 1).charAt(0);
                    if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') { i--; continue; }
                    return false; // es steht schon etwas vor dem Cursor → weiterer Parameter
                }
            } catch (BadLocationException ignored) { }
            return true; // nur Whitespace zwischen '(' und Cursor
        }

        private boolean isWs(char ch) {
            return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
        }

        private int lastIndexOf(String s, String needle, int upto) {
            int stop = Math.min(upto, s.length());
            if (stop <= 0) return -1;
            return s.lastIndexOf(needle, stop - 1);
        }
    }





    // ---------- TableCellEditor ----------

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        textArea.setText(value != null ? value.toString() : "");
        textArea.selectAll();
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                textArea.requestFocusInWindow();
                autoCompletion.doCompletion();
            }
        });
        return textArea;
    }

    @Override
    public Object getCellEditorValue() {
        return textArea.getText();
    }

    // --- Tabbed Popup Window ----------------------------------------------------
    // --- Tabbed Popup Window mit rechter Description-Spalte (RSyntax/AutoComplete API kompatibel) ---
    final class TabbedAutoCompletePopupWindow extends JWindow {

        private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        private final JList<Object> fnList   = new JList<Object>();
        private final JList<Object> varList  = new JList<Object>();
        private final JList<Object> rxList   = new JList<Object>();

        private final DefaultListModel<Object> fnModel  = new DefaultListModel<Object>();
        private final DefaultListModel<Object> varModel = new DefaultListModel<Object>();
        private final DefaultListModel<Object> rxModel  = new DefaultListModel<Object>();

        private final AutoCompletion ac;

        // Right side: HTML description
        private final JEditorPane descPane = new JEditorPane("text/html", "");
        private final JScrollPane descScroll = new JScrollPane(descPane);

        TabbedAutoCompletePopupWindow(Window owner, AutoCompletion ac,
                                      String fnTitle, String varTitle, String rxTitle) {
            super(owner);
            this.ac = ac;

            fnList.setModel(fnModel);
            varList.setModel(varModel);
            rxList.setModel(rxModel);

            @SuppressWarnings("unchecked")
            ListCellRenderer<Object> r = ac.getListCellRenderer();
            if (r != null) {
                fnList.setCellRenderer(r);
                varList.setCellRenderer(r);
                rxList.setCellRenderer(r);
            }

            // Double-click or Enter -> insert selection
            MouseAdapter dbl = new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) insertSelected();
                }
            };
            KeyAdapter enter = new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) insertSelected();
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) setVisible(false);
                }
            };
            fnList.addMouseListener(dbl); varList.addMouseListener(dbl); rxList.addMouseListener(dbl);
            fnList.addKeyListener(enter); varList.addKeyListener(enter); rxList.addKeyListener(enter);

            // Selection -> update description
            javax.swing.event.ListSelectionListener sel = new javax.swing.event.ListSelectionListener() {
                public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) updateDescription(getSelection());
                }
            };
            fnList.addListSelectionListener(sel);
            varList.addListSelectionListener(sel);
            rxList.addListSelectionListener(sel);

            tabs.addTab(fnTitle,  new JScrollPane(fnList));
            tabs.addTab(varTitle, new JScrollPane(varList));
            tabs.addTab(rxTitle,  new JScrollPane(rxList));

            descPane.setEditable(false);
            descPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabs, descScroll);
            split.setResizeWeight(0.6);
            split.setBorder(BorderFactory.createEmptyBorder());

            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(split, BorderLayout.CENTER);

            setSize(640, 280);
        }

        void selectTab(int index) {
            if (index >= 0 && index < tabs.getTabCount()) tabs.setSelectedIndex(index);
        }

        void setCompletions(java.util.List<Completion> completions) {
            fnModel.clear(); varModel.clear(); rxModel.clear();
            for (int i = 0; i < completions.size(); i++) {
                Completion c = completions.get(i);
                if (c instanceof FunctionCompletion || c instanceof TemplateCompletion || c instanceof ParameterizedCompletion) {
                    fnModel.addElement(c);
                } else if (c instanceof BasicCompletion) {
                    String simple = c.getClass().getSimpleName();
                    if (simple.contains("Variable")) varModel.addElement(c);
                    else if (simple.contains("Regex")) rxModel.addElement(c);
                    else varModel.addElement(c);
                } else {
                    varModel.addElement(c);
                }
            }
            if (fnModel.size() > 0) tabs.setSelectedIndex(0);
            else if (varModel.size() > 0) tabs.setSelectedIndex(1);
            else tabs.setSelectedIndex(2);

            updateDescription(getSelection());
        }

        void setLocationRelativeToCaret(JTextComponent tc) throws BadLocationException {
            Rectangle r = tc.modelToView(tc.getCaretPosition());
            if (r == null) return;
            Point p = new Point(r.x, r.y + r.height);
            SwingUtilities.convertPointToScreen(p, tc);
            setLocation(p);
        }

        Completion getSelection() {
            int idx = tabs.getSelectedIndex();
            if (idx == 0) return (Completion) fnList.getSelectedValue();
            if (idx == 1) return (Completion) varList.getSelectedValue();
            return (Completion) rxList.getSelectedValue();
        }

        private void insertSelected() {
            Completion c = getSelection();
            if (c != null) {
                setVisible(false);
                ((TabbedAutoCompletion) ac).performInsertion(c);
            }
        }

        // ------ Description rendering (nutzt getSummary / getToolTipText) -------

        private void updateDescription(Completion c) {
            if (c == null) { descPane.setText(""); return; }
            descPane.setText(buildHtml(c));
            descPane.setCaretPosition(0);
        }

        private String buildHtml(Completion c) {
            // Try rich HTML summary first (FunctionCompletion liefert hier Doku inkl. Parametern)
            String summary = safe(c.getSummary());
            if (summary != null && summary.length() > 0) {
                // Wrap to ensure consistent font
                return wrap(summary);
            }

            // Fallback: tooltip text
            String tip = safe(c.getToolTipText());
            if (tip != null && tip.length() > 0) {
                return wrap(tip);
            }

            // Minimal rendering: name + (param list) selbst ableiten, falls FunctionCompletion
            StringBuilder sb = new StringBuilder(256);
            sb.append("<html><body style='font-family: sans-serif; font-size:12px;'>");
            sb.append("<div style='font-weight:bold; font-size:13px; margin-bottom:6px;'>")
                    .append(escape(displayName(c))).append("</div>");

            if (c instanceof FunctionCompletion) {
                FunctionCompletion fc = (FunctionCompletion) c;
                int n = fc.getParamCount();
                if (n > 0) {
                    sb.append("<div><code>(");
                    for (int i = 0; i < n; i++) {
                        ParameterizedCompletion.Parameter p = fc.getParam(i);
                        if (i > 0) sb.append("; ");
                        String pn = (p.getName() != null) ? p.getName()
                                : (p.getType() != null) ? p.getType()
                                : ("arg" + (i + 1));
                        sb.append(escape(pn));
                    }
                    sb.append(")</code></div>");
                }
            } else {
                sb.append("<div style='color:#777;'>No description available.</div>");
            }

            sb.append("</body></html>");
            return sb.toString();
        }

        private String wrap(String innerHtml) {
            // Ensure consistent fonts even if summary already contains <html>...
            return "<html><body style='font-family: sans-serif; font-size:12px;'>" + innerHtml + "</body></html>";
        }

        private String displayName(Completion c) {
            String n = c.getReplacementText();
            if (n == null || n.length() == 0) n = c.toString();
            if (n.startsWith("{{") && n.endsWith("}}")) {
                int lp = n.indexOf('(');
                if (lp > 2) n = n.substring(2, lp);
            }
            return n;
        }

        private String safe(String s) { return s; } // Summary ist bereits HTML.

        private String escape(String s) {
            if (s == null) return "";
            StringBuilder out = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                switch (ch) {
                    case '&': out.append("&amp;"); break;
                    case '<': out.append("&lt;"); break;
                    case '>': out.append("&gt;"); break;
                    case '"': out.append("&quot;"); break;
                    case '\'': out.append("&#39;"); break;
                    default: out.append(ch);
                }
            }
            return out.toString();
        }
    }

    // --- AutoCompletion mit Tabbed Popup ---------------------------------------
    final class TabbedAutoCompletion extends CursorAutoCompletion {

        private TabbedAutoCompletePopupWindow popup;
        private final String fnTitle;
        private final String varTitle;
        private final String rxTitle;

        TabbedAutoCompletion(CompletionProvider provider, String fnTitle, String varTitle, String rxTitle) {
            super(provider);
            this.fnTitle = fnTitle;
            this.varTitle = varTitle;
            this.rxTitle = rxTitle;
            setAutoCompleteSingleChoices(false);
            setShowDescWindow(false);
        }

        // Öffentliche Brücke für das Popup (ruft die protected Basismethode auf)
        public void performInsertion(Completion c) {
            super.insertCompletion(c);
        }

        @Override
        public void install(JTextComponent c) {
            super.install(c);
            Window owner = SwingUtilities.getWindowAncestor(c);
            if (owner == null) owner = JOptionPane.getRootFrame();
            popup = new TabbedAutoCompletePopupWindow(owner, this, fnTitle, varTitle, rxTitle);
        }

        @Override
        protected int refreshPopupWindow() {
            JTextComponent tc = getTextComponent();
            if (tc == null) return 0;

            java.util.List<Completion> completions = getCompletionProvider().getCompletions(tc);
            int count = completions == null ? 0 : completions.size();
            if (count <= 0) {
                hidePopupWindow();
                return 0;
            }

            if (popup == null) {
                Window owner = SwingUtilities.getWindowAncestor(tc);
                if (owner == null) owner = JOptionPane.getRootFrame();
                popup = new TabbedAutoCompletePopupWindow(owner, this, fnTitle, varTitle, rxTitle);
            }

            popup.setCompletions(completions);
            // Decide default tab by context: if caret is inside fn(...), prefer Variables tab
            boolean inArgs = false;
            try {
                int pos = tc.getCaretPosition();
                Bounds b = findFnBoundsAt(tc.getDocument(), pos); // inherited from CursorAutoCompletion
                inArgs = (b != null && pos >= b.parenOpen + 1 && pos <= b.parenClose);
            } catch (Exception ignore) {
                // keep default
            }

            // 0 = Functions, 1 = Variables, 2 = Regex
            popup.selectTab(inArgs ? 1 : 0);

            try {
                popup.setLocationRelativeToCaret(tc);
            } catch (BadLocationException ignore) { }
            popup.setVisible(true);
            return 0; // nicht getLineOfCaret() benutzen (package-private)
        }

        @Override
        protected boolean hidePopupWindow() {
            if (popup != null && popup.isVisible()) {
                popup.setVisible(false);
                return true;
            }
            return false;
        }
    }



}
