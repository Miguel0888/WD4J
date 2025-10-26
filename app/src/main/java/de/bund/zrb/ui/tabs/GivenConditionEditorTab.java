package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.GivenRegistry;
import de.bund.zrb.model.GivenTypeDefinition;
import de.bund.zrb.model.GivenTypeDefinition.GivenField;
import de.bund.zrb.model.Precondition;
import de.bund.zrb.service.PreconditionRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.service.UserRegistry.User;
import de.bund.zrb.ui.expressions.ExpressionSelectionListener;
import de.bund.zrb.ui.expressions.ExpressionTreeComboBox;
import de.bund.zrb.ui.expressions.ExpressionTreeNode;
import de.bund.zrb.ui.parts.Code;
import de.bund.zrb.expressions.domain.ResolvableExpression;
import de.bund.zrb.expressions.engine.ExpressionParser;
import de.bund.zrb.expressions.engine.CompositeExpression;
import de.bund.zrb.expressions.engine.FunctionExpression;
import de.bund.zrb.expressions.engine.VariableExpression;
import de.bund.zrb.expressions.engine.LiteralExpression;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Editor für eine einzelne GivenCondition.
 *
 * Responsibilities:
 * - Wähle Given-Typ (aus GivenRegistry).
 * - Wähle User (username wird gespeichert).
 * - Bearbeite parametrisierte Felder des Given-Typs.
 * - NEU: Definiere eine fachliche Expression (z. B. "Es existiert eine {{Belegnummer}}.")
 *   und binde einen konkreten AST-Knoten daraus (z. B. "Variable: Belegnummer")
 *   über ein Baum-Dropdown.
 *
 * Persistenz:
 * - Wir schreiben alles nach condition.setValue(...) als key=value&key=value...
 *   inkl.:
 *   - username
 *   - alle dynamischen Felder
 *   - expressionRaw        (kompletter Ausdruckstext)
 *   - expressionBinding    (Label des im Baum gewählten Knotens)
 *
 * Hinweis zur Laufzeit:
 * - chosenExpressionNode (ResolvableExpression) wird NICHT hier serialisiert.
 *   Das später in ScenarioState abzulegen ist der nächste Schritt.
 */
public class GivenConditionEditorTab extends JPanel {

    private static final String TYPE_PRECONDITION_REF = "preconditionRef";

    private final GivenCondition condition;
    private final JComboBox<String> typeBox;
    private final JComboBox<String> userBox;

    private final JPanel dynamicFieldsPanel = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> inputs = new LinkedHashMap<String, JComponent>();

    // Expression-spezifische Felder
    // statt TextField jetzt eine editable ComboBox mit Vorschlägen
    private JComboBox<String> expressionInputBox;
    private ExpressionTreeComboBox astComboBox;

    private ResolvableExpression chosenExpressionNode;
    private String chosenExpressionLabel;

    public GivenConditionEditorTab(GivenCondition condition) {
        this.condition = condition;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Kopfbereich: Typ + User
        JPanel header = new JPanel(new GridLayout(0, 2, 8, 8));

        header.add(new JLabel("Typ:"));
        String[] types = GivenRegistry.getInstance().getAll()
                .stream().map(GivenTypeDefinition::getType).toArray(String[]::new);
        typeBox = new JComboBox<String>(types);
        typeBox.setEditable(true);
        typeBox.setSelectedItem(condition.getType());
        header.add(typeBox);

        header.add(new JLabel("User:"));
        String[] users = UserRegistry.getInstance().getAll().stream()
                .map(User::getUsername).toArray(String[]::new);
        userBox = new JComboBox<String>(users);

        String initialUser = (String) parseValueMap(condition.getValue()).get("username");
        if (initialUser != null && initialUser.trim().length() > 0) {
            userBox.setSelectedItem(initialUser.trim());
        }
        header.add(userBox);

        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.add(header, BorderLayout.NORTH);
        form.add(dynamicFieldsPanel, BorderLayout.CENTER);
        add(form, BorderLayout.CENTER);

        typeBox.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                rebuildDynamicForm(String.valueOf(typeBox.getSelectedItem()));
            }
        });

        JButton saveBtn = new JButton("Speichern");
        saveBtn.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(saveBtn);
        add(footer, BorderLayout.SOUTH);

        rebuildDynamicForm(condition.getType());
    }

    /**
     * Baue dynamische Felder je nach Given-Typ neu auf.
     * Zusätzlich wird am Ende die neue "Expression"-Sektion aufgebaut:
     * - Editable ComboBox mit Vorschlägen
     * - AST-Dropdown (Tree)
     *
     * Und: Wir hängen Listener an, sodass Änderungen im Expression-Combo
     * sofort den Baum neu parsen.
     */
    private void rebuildDynamicForm(String type) {
        dynamicFieldsPanel.removeAll();
        inputs.clear();

        GivenTypeDefinition def = GivenRegistry.getInstance().get(type);
        Map<String, Object> paramMap = parseValueMap(condition.getValue());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        // ---------------------------------------------------------------------
        // 1. Standard-Felder aus GivenTypeDefinition
        // ---------------------------------------------------------------------
        if (def != null) {
            for (GivenField field : def.getFields().values()) {

                Object value = paramMap.get(field.name);
                if (value == null && field.defaultValue != null) {
                    value = field.defaultValue;
                }

                // Code-Editor (mehrzeilig)
                if (field.type == Code.class) {
                    gbc.gridx = 0; gbc.gridy = row++;
                    gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0;
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    dynamicFieldsPanel.add(new JLabel(field.label), gbc);

                    RSyntaxTextArea editor = new RSyntaxTextArea(10, 40);
                    editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                    editor.setCodeFoldingEnabled(true);
                    editor.setText(value != null ? value.toString() : "");
                    RTextScrollPane scrollPane = new RTextScrollPane(editor);

                    gbc.gridy = row++; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
                    dynamicFieldsPanel.add(scrollPane, gbc);

                    inputs.put(field.name, editor);
                    continue;
                }

                // Spezialfall: PreconditionRef.id als Dropdown
                if (TYPE_PRECONDITION_REF.equals(def.getType()) && "id".equals(field.name)) {
                    gbc.gridx = 0; gbc.gridy = row;
                    gbc.gridwidth = 1; gbc.weightx = 0; gbc.weighty = 0;
                    gbc.fill = GridBagConstraints.NONE;
                    dynamicFieldsPanel.add(new JLabel(field.label + ":"), gbc);

                    gbc.gridx = 1; gbc.weightx = 1.0;
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    JComboBox<PreItem> preBox = new JComboBox<PreItem>();

                    List<Precondition> pres = PreconditionRegistry.getInstance().getAll();
                    PreItem selected = null;
                    for (int i = 0; i < pres.size(); i++) {
                        Precondition p = pres.get(i);
                        String id = p.getId();
                        String name = (p.getName() != null && p.getName().trim().length() > 0)
                                ? p.getName().trim() : "(unnamed)";
                        PreItem item = new PreItem(id, name);
                        preBox.addItem(item);

                        if (value != null && value.equals(id)) {
                            selected = item;
                        }
                    }
                    if (selected != null) {
                        preBox.setSelectedItem(selected);
                    }

                    dynamicFieldsPanel.add(preBox, gbc);
                    inputs.put(field.name, preBox);

                    row++;
                    continue;
                }

                // Standard: 1-zeiliges Textfeld
                gbc.gridx = 0; gbc.gridy = row;
                gbc.gridwidth = 1; gbc.weightx = 0; gbc.weighty = 0;
                gbc.fill = GridBagConstraints.NONE;
                dynamicFieldsPanel.add(new JLabel(field.label + ":"), gbc);

                gbc.gridx = 1; gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                JTextField tf = new JTextField(value != null ? value.toString() : "");
                dynamicFieldsPanel.add(tf, gbc);
                inputs.put(field.name, tf);

                row++;
            }
        }

        // ---------------------------------------------------------------------
        // 2. NEU: Expression-Zeile
        //
        //    Links: Label "Expression:"
        //    Rechts: Panel mit:
        //        a) expressionInputBox (editable ComboBox mit Vorschlägen)
        //        b) astComboBox (unser Baum-Dropdown)
        //
        //    Verhalten:
        //    - Wenn der Nutzer in expressionInputBox tippt oder etwas auswählt,
        //      parsen wir sofort neu und aktualisieren astComboBox.
        //
        //    Wir ziehen initialRaw / initialBindingLabel aus condition.getValue().
        // ---------------------------------------------------------------------

        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 1; gbc.weightx = 0; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        dynamicFieldsPanel.add(new JLabel("Expression:"), gbc);

        JPanel exprPanel = new JPanel(new BorderLayout(4, 4));

        // Bereits gespeicherte Werte holen
        String initialRaw = asString(paramMap.get("expressionRaw"));
        String initialBindingLabel = asString(paramMap.get("expressionBinding"));
        chosenExpressionLabel = initialBindingLabel;

        // a) editable ComboBox mit Vorschlägen
        expressionInputBox = new JComboBox<String>();
        expressionInputBox.setEditable(true);

        // Vorschläge für typische Givens / Step-Texte
        addIfAbsent(expressionInputBox, "Es existiert eine {{Belegnummer}}.");
        addIfAbsent(expressionInputBox, "{{OTP({{username}})}}");
        addIfAbsent(expressionInputBox, "{{Uhrzeit}}");

        // Falls es schon gespeicherte ExpressionRaw gab, ebenfalls als Option anbieten
        if (initialRaw != null && initialRaw.trim().length() > 0) {
            addIfAbsent(expressionInputBox, initialRaw.trim());
            expressionInputBox.setSelectedItem(initialRaw.trim());
        } else {
            // ansonsten Standardvorschlag vorselektieren (damit du direkt was siehst)
            expressionInputBox.setSelectedItem("Es existiert eine {{Belegnummer}}.");
        }

        exprPanel.add(expressionInputBox, BorderLayout.NORTH);

        // b) unser Tree-Dropdown
        astComboBox = new ExpressionTreeComboBox();

        // AST initial aus dem (ggf. vorbefüllten) expressionInputBox-Wert bauen
        refreshAstFromCurrentExpression();

        // falls es schon ein gespeichertes Binding-Label gab,
        // setze dieses Label als sichtbaren Text der astComboBox
        if (initialBindingLabel != null && initialBindingLabel.trim().length() > 0) {
            presetComboDisplay(astComboBox, initialBindingLabel.trim());
        }

        // Listener: Benutzer klickt auf einen Knoten in der Baum-Combo
        astComboBox.addSelectionListener(new ExpressionSelectionListener() {
            public void onExpressionSelected(String chosenLabel,
                                             ResolvableExpression chosenExpr) {
                // Merke die konkrete AST-Node (für spätere Laufzeit)
                chosenExpressionNode = chosenExpr;
                chosenExpressionLabel = chosenLabel;
            }
        });

        exprPanel.add(astComboBox, BorderLayout.CENTER);

        // Listener auf expressionInputBox:
        // - Bei Änderung (z. B. Nutzer tippt neuen Text oder wählt was anderes),
        //   bitte Baum neu parsen und anzeigen.
        expressionInputBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                // Reagiere nur auf "selected", nicht auf "deselected"
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    refreshAstFromCurrentExpression();
                }
            }
        });
        // Zusätzlich auf Focus-Loss für eigene Eingaben (User hat editiert und verlässt das Feld)
        expressionInputBox.getEditor().getEditorComponent().addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                refreshAstFromCurrentExpression();
            }
        });

        gbc.gridx = 1; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        dynamicFieldsPanel.add(exprPanel, gbc);

        // Wir wollen expressionRaw beim Speichern erfassen
        inputs.put("expressionRaw", expressionInputBox);

        row++;

        dynamicFieldsPanel.revalidate();
        dynamicFieldsPanel.repaint();
    }

    /**
     * Parse den aktuellen Text aus expressionInputBox, und aktualisiere astComboBox.setTreeRoot(...)
     * Der Baum zeigt dann echte Knoten wie:
     *
     * Composite
     *   Literal: "Es existiert eine "
     *   Variable: Belegnummer
     *   Literal: "."
     */
    private void refreshAstFromCurrentExpression() {
        if (astComboBox == null || expressionInputBox == null) {
            return;
        }

        String raw = stringValue(expressionInputBox.getEditor().getItem());
        ExpressionTreeNode uiRoot = buildTreeFromRawExpression(raw);
        astComboBox.setTreeRoot(uiRoot);

        // Wenn wir neu parsen, ist die alte Auswahl eigentlich semantisch ungültig,
        // deshalb setzen wir chosenExpressionNode bewusst NICHT automatisch neu.
        // Das erzwingt, dass der Tester aktiv wieder klickt, falls sich die Struktur geändert hat.
    }

    /**
     * Nimmt einen beliebigen Freitext und versucht, ihn mit dem ExpressionParser
     * in einen AST zu übersetzen. Schlägt das fehl, wird ein "ERROR"-Baum gebaut.
     */
    private ExpressionTreeNode buildTreeFromRawExpression(String raw) {
        if (raw == null || raw.trim().length() == 0) {
            return buildFallbackNoDataTree();
        }

        try {
            ExpressionParser parser = new ExpressionParser();
            ResolvableExpression expr = parser.parseTemplate(raw);
            return ExpressionTreeNode.fromExpression(expr);

        } catch (Exception ex) {
            return buildErrorTree(raw, ex.getMessage());
        }
    }

    /**
     * Fallback-Baum für leere Eingaben.
     */
    private ExpressionTreeNode buildFallbackNoDataTree() {
        LiteralExpression lit = new LiteralExpression("No data");
        return new ExpressionTreeNode(
                lit,
                "Literal: \"No data\"",
                java.util.Collections.<ExpressionTreeNode>emptyList()
        );
    }

    /**
     * Fehler-Baum, falls der Parser die Eingabe nicht versteht.
     */
    private ExpressionTreeNode buildErrorTree(String raw, String msg) {
        LiteralExpression lit = new LiteralExpression("Parse Error: " + msg);
        return new ExpressionTreeNode(
                lit,
                "ERROR parsing: " + raw,
                java.util.Collections.<ExpressionTreeNode>emptyList()
        );
    }

    /**
     * Speichere die Änderungen zurück in das GivenCondition-Objekt.
     *
     * Wir sammeln:
     * - username
     * - alle dynamischen Felder
     * - expressionRaw (aktueller Text aus expressionInputBox)
     * - expressionBinding (Label vom gewählten AST-Knoten aus astComboBox)
     */
    private void save() {
        // Typ persistieren
        condition.setType(String.valueOf(typeBox.getSelectedItem()));

        Map<String, String> result = new LinkedHashMap<String, String>();

        // username
        Object u = userBox.getSelectedItem();
        if (u != null && u.toString().trim().length() > 0) {
            result.put("username", u.toString().trim());
        }

        // dynamische Felder inkl. expressionRaw
        for (Map.Entry<String, JComponent> entry : inputs.entrySet()) {
            String name = entry.getKey();
            JComponent input = entry.getValue();

            if (input instanceof JTextField) {
                result.put(name, ((JTextField) input).getText());
            } else if (input instanceof RSyntaxTextArea) {
                result.put(name, ((RSyntaxTextArea) input).getText());
            } else if (input instanceof JComboBox) {
                // expressionRaw steckt jetzt in einer JComboBox (editable)
                JComboBox box = (JComboBox) input;
                Object editorVal = box.getEditor().getItem();
                result.put(name, stringValue(editorVal));
            }
        }

        // expressionBinding -> welches AST-Node-Label hat der User gewählt?
        if (chosenExpressionLabel != null && chosenExpressionLabel.trim().length() > 0) {
            result.put("expressionBinding", chosenExpressionLabel.trim());
        }

        condition.setValue(serializeValueMap(result));
        TestRegistry.getInstance().save();

        JOptionPane.showMessageDialog(
                this,
                "Änderungen gespeichert.\n" +
                        "expressionRaw = " + result.get("expressionRaw") + "\n" +
                        "expressionBinding = " + result.get("expressionBinding")
        );
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden / Utils
    // -------------------------------------------------------------------------

    private Map<String, Object> parseValueMap(String value) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (value != null && value.contains("=")) {
            String[] pairs = value.split("&");
            for (int i = 0; i < pairs.length; i++) {
                String pair = pairs[i];
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    result.put(kv[0], kv[1]);
                }
            }
        }
        return result;
    }

    private String serializeValueMap(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

    /** Für preconditionRef.id: "Name {UUID}" anzeigen, aber nur UUID speichern. */
    private static final class PreItem {
        final String id;
        final String name;
        PreItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
        public String toString() {
            return name + " {" + id + "}";
        }
    }

    private String asString(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }

    private String stringValue(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }

    /**
     * Setze ein Label als sichtbaren Text im astComboBox-"Feld", ohne neu auszuwählen.
     * (Gleicher Hack wie im ActionEditorTab.)
     */
    private void presetComboDisplay(ExpressionTreeComboBox combo, String label) {
        JTextField f = findInternalDisplayField(combo);
        if (f != null) {
            f.setText(label);
            f.putClientProperty("chosenExpr", null);
            f.putClientProperty("chosenLabel", label);
        }
    }

    /**
     * Finde das innere, nicht-editierbare Anzeige-Textfeld der ExpressionTreeComboBox.
     */
    private JTextField findInternalDisplayField(ExpressionTreeComboBox combo) {
        Component[] kids = combo.getComponents();
        for (int i = 0; i < kids.length; i++) {
            if (kids[i] instanceof JTextField) {
                return (JTextField) kids[i];
            }
        }
        return null;
    }

    /**
     * Füge value der ComboBox hinzu, falls noch nicht vorhanden.
     * (Verhindere Duplikate in der Vorschlagsliste.)
     */
    private void addIfAbsent(JComboBox<String> box, String value) {
        if (value == null) return;
        String v = value.trim();
        if (v.length() == 0) return;

        int count = box.getItemCount();
        for (int i = 0; i < count; i++) {
            Object it = box.getItemAt(i);
            if (v.equals(it)) {
                return;
            }
        }
        box.addItem(v);
    }
}
