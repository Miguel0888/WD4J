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
        provider.setAutoActivationRules(true,
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_({[\"';*}]) ");

        // Eigene AutoCompletion, die Replacement kontextsensitiv formatiert und Caret setzt
        autoCompletion = new CursorAutoCompletion(provider);
        autoCompletion.setAutoActivationEnabled(true);
        autoCompletion.setAutoActivationDelay(80);
        autoCompletion.setParameterAssistanceEnabled(false); // vermeide zusätzliches "()"
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
                int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(); // Java 8
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
            @Override public void focusGained(FocusEvent e) {
                ac.doCompletion();
            }
        });
        ta.addCaretListener(new javax.swing.event.CaretListener() {
            @Override public void caretUpdate(javax.swing.event.CaretEvent e) {
                ac.doCompletion();
            }
        });
        ta.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_ESCAPE) {
                    ac.doCompletion();
                }
            }
        });
    }

    // ---------- Provider: liefert IMMER Variablen, Funktionen und Regex ----------

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

        @Override public char   getParameterListStart()       { return '\0'; } // keine automatische "(" Action
        @Override public String getParameterListSeparator()   { return "; "; }
        @Override public char   getParameterListEnd()         { return ')'; }

        @Override
        protected boolean isValidChar(char ch) {
            return Character.isLetterOrDigit(ch) || ch == '_' || ch == '*' || ch == '"' || ch == '{' || ch == '}';
        }

        @Override
        public boolean isAutoActivateOkay(JTextComponent tc) { return true; }

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
                    } else {
                        break;
                    }
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

            // Variablen
            List<String> vars = variableNamesSupplier.get();
            for (int i = 0; i < vars.size(); i++) {
                String v = vars.get(i);
                if (prefix.length() == 0 || startsWithIgnoreCase(v, prefix)) {
                    out.add(new VariableCompletion(this, v));
                }
            }

            // Funktionen
            Map<String, DescribedItem> fmap = functionItemsSupplier.get();
            List<String> fnNames = sortedKeys(fmap.keySet());
            for (int i = 0; i < fnNames.size(); i++) {
                String fn = fnNames.get(i);
                if (prefix.length() == 0 || startsWithIgnoreCase(fn, prefix)) {
                    String desc = fmap.containsKey(fn) ? fmap.get(fn).getDescription() : null;
                    out.add(new FunctionCompletionWrapped(this, fn, desc));
                }
            }

            // Regex
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

    /** Variable → Standard ist {{name}}; Cursor/AutoCompletion entscheiden bei Bedarf Kontextformat */
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

    /** Funktion → immer {{name()}}; Caret nach '(' */
    private static final class FunctionCompletionWrapped extends FunctionCompletion {
        private final String fn;
        FunctionCompletionWrapped(CompletionProvider provider, String functionName, String description) {
            super(provider, functionName, null);
            this.fn = functionName;
            if (description != null && description.length() > 0) {
                setShortDescription(description);
            }
        }
        @Override public String getInputText() { return "<html><b>" + escapeHtml(fn) + "</b></html>"; }
        @Override public String getReplacementText() { return "{{" + fn + "()}}"; }
        String getFunctionName() { return fn; } // expose for caret calc
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

    /** Regex → Standard ist "pattern"; Cursor/AutoCompletion entscheidet bei Bedarf Kontextformat */
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

    /**
     * AutoCompletion mit:
     *  - kontextsensitivem Replacement (Variablen/Regex in Funktionsargumenten → "…"; )
     *  - Caret-Positionierung nach Funktionsinsert (nach '(')
     */
    private static class CursorAutoCompletion extends AutoCompletion {

        CursorAutoCompletion(CompletionProvider provider) {
            super(provider);
        }

        @Override
        protected String getReplacementText(Completion c, Document doc, int start, int len) {
            // Entscheide kontextsensitiv
            if (c instanceof VariableCompletion || c instanceof RegexCompletion) {
                // Wenn Caret innerhalb einer Funktionsargumentliste (…( | )… ) steht, formatiere "…";
                if (isInsideFunctionArgs(doc, start)) {
                    if (c instanceof VariableCompletion) {
                        VariableCompletion vc = (VariableCompletion) c;
                        return "\"" + "{{" + vc.getVariableName() + "}}" + "\"; ";
                    } else {
                        RegexCompletion rc = (RegexCompletion) c;
                        // rc liefert bereits "pattern" → ergänze ; und Leerzeichen
                        String base = rc.getReplacementText();
                        return base + "; ";
                    }
                }
            }
            // Default-Verhalten
            return c.getReplacementText();
        }

        @Override
        protected void insertCompletion(Completion c, boolean typedParamListStartChar) {
            // Standardinsert (mit unserem getReplacementText) + Caretsteuerung für Funktion
            JTextComponent textComp = getTextComponent();
            String alreadyEntered = c.getAlreadyEntered(textComp);

            hideChildWindows();

            javax.swing.text.Caret caret = textComp.getCaret();
            int dot = caret.getDot();
            int len = alreadyEntered != null ? alreadyEntered.length() : 0;
            int start = dot - len;

            Document doc = textComp.getDocument();
            String replacement = getReplacementText(c, doc, start, len);

            caret.setDot(start);
            caret.moveDot(dot);
            textComp.replaceSelection(replacement);

            if (c instanceof FunctionCompletionWrapped) {
                FunctionCompletionWrapped fc = (FunctionCompletionWrapped) c;
                int caretPos = start + 2 + fc.getFunctionName().length() + 1; // "{{" + fn + "("
                caret.setDot(caretPos);
            }
        }

        // --- Kontext-Detektion: Steht Einfügepunkt in {{fn(|)}} Argumentliste? ---
        private boolean isInsideFunctionArgs(Document doc, int insertStart) {
            try {
                String all = doc.getText(0, doc.getLength());

                // Finde das letzte "{{" vor insertStart
                int openIdx = lastIndexOf(all, "{{", insertStart);
                if (openIdx < 0) return false;

                // Finde die nächste "}}" nach openIdx
                int closeIdx = all.indexOf("}}", openIdx + 2);
                if (closeIdx < 0) return false;

                // Insert muss zwischen openIdx und closeIdx liegen
                if (insertStart < openIdx || insertStart > closeIdx) return false;

                // Innerhalb dieses Blocks muss eine '(' vor insertStart und eine ')' nach insertStart liegen
                int parenOpen = all.indexOf('(', openIdx + 2);
                if (parenOpen < 0 || parenOpen >= insertStart) return false;

                int parenClose = all.indexOf(')', parenOpen + 1);
                if (parenClose < 0 || parenClose < insertStart) return false;

                return true;
            } catch (BadLocationException e) {
                return false;
            }
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
