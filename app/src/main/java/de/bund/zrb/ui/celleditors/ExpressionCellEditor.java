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

        autoCompletion = new CursorAutoCompletion(provider);
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

    private static final class FunctionCompletionWrapped extends FunctionCompletion {
        private final String fn;
        FunctionCompletionWrapped(CompletionProvider provider, String functionName, String description) {
            super(provider, functionName, null);
            this.fn = functionName;
            if (description != null && description.length() > 0) setShortDescription(description);
        }
        @Override public String getInputText() { return "<html><b>" + escapeHtml(fn) + "</b></html>"; }
        @Override public String getReplacementText() { return "{{" + fn + "()}}"; }
        String getFunctionName() { return fn; }
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

            // Kontext: Variable/Regex innerhalb von {{fn( … )}} ?
            if ((c instanceof VariableCompletion) || (c instanceof RegexCompletion)) {
                Bounds b = findFnBoundsAt(tc.getDocument(), pos);
                if (b != null && pos >= b.parenOpen + 1 && pos <= b.parenClose) {

                    // Bestimme, ob erster oder weiterer Parameter (nur Links-Scan)
                    boolean firstParam = isFirstParamPosition(b, pos, tc.getDocument());

                    StringBuilder sb = new StringBuilder();
                    if (!firstParam) {
                        sb.append("; ");
                    }

                    if (c instanceof VariableCompletion) {
                        VariableCompletion vc = (VariableCompletion) c;
                        sb.append("\"{{").append(vc.getVariableName()).append("}}\"");
                    } else {
                        sb.append(((RegexCompletion) c).getReplacementText()); // already quoted
                    }

                    tc.replaceSelection(sb.toString());
                    // Caret bleibt einfach hinter dem eingefügten Token
                    return;
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

        private static final class Bounds {
            String text;
            int blockOpen;
            int blockClose;
            int parenOpen;
            int parenClose;
        }

        /** Finde den umgebenden {{fn(...)}}-Block für 'pos' per balanciertem Scan. */
        private Bounds findFnBoundsAt(Document doc, int pos) {
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
}
