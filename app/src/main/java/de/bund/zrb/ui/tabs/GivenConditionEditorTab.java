package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.*;
import de.bund.zrb.model.GivenTypeDefinition.GivenField;
import de.bund.zrb.service.PreconditionRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.service.UserRegistry.User;
import de.bund.zrb.ui.parts.Code;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Editor für eine GivenCondition.
 * - Typ-Auswahl aus GivenRegistry
 * - Oben immer: "User:"-Dropdown -> speichert "username" im Param-String
 * - Dynamische Felder je Typ
 * - Spezialfall preconditionRef: Feld "id" als Precondition-Dropdown (Name {UUID}) statt Freitext
 */
public class GivenConditionEditorTab extends JPanel {

    private static final String TYPE_PRECONDITION_REF = "preconditionRef";

    private final GivenCondition condition;
    private final JComboBox<String> typeBox;
    private final JPanel dynamicFieldsPanel = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> inputs = new LinkedHashMap<String, JComponent>();

    private JComboBox<String> userBox;

    public GivenConditionEditorTab(GivenCondition condition) {
        this.condition = condition;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Typen aus Registry
        String[] types = GivenRegistry.getInstance().getAll()
                .stream().map(GivenTypeDefinition::getType).toArray(String[]::new);
        typeBox = new JComboBox<String>(types);
        typeBox.setSelectedItem(condition.getType());
        typeBox.addActionListener(e -> rebuildDynamicForm((String) typeBox.getSelectedItem()));

        JPanel form = new JPanel(new BorderLayout(8, 8));
        JPanel typeRow = new JPanel(new GridLayout(1, 2, 8, 8));
        typeRow.add(new JLabel("Typ:"));
        typeRow.add(typeBox);
        form.add(typeRow, BorderLayout.NORTH);

        form.add(dynamicFieldsPanel, BorderLayout.CENTER);
        add(form, BorderLayout.CENTER);

        JButton saveBtn = new JButton("Speichern");
        saveBtn.addActionListener(this::save);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(saveBtn);
        add(footer, BorderLayout.SOUTH);

        rebuildDynamicForm(condition.getType());
    }

    private void rebuildDynamicForm(String type) {
        dynamicFieldsPanel.removeAll();
        inputs.clear();

        Map<String, Object> paramMap = parseValueMap(condition.getValue());
        GridBagConstraints gbc = baseGbc();
        int row = 0;

        // ---------- (1) Immer oben: User ----------
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        gbc.weightx = 0; gbc.weighty = 0; gbc.fill = GridBagConstraints.NONE;
        dynamicFieldsPanel.add(new JLabel("User:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        String[] users = UserRegistry.getInstance().getAll().stream()
                .map(User::getUsername).toArray(String[]::new);
        userBox = new JComboBox<String>(users);
        String preUser = (String) paramMap.get("username");
        if (preUser != null && preUser.trim().length() > 0) {
            userBox.setSelectedItem(preUser.trim());
        }
        dynamicFieldsPanel.add(userBox, gbc);
        row++;

        // ---------- (2) Dynamische Felder nach Typ ----------
        GivenTypeDefinition def = GivenRegistry.getInstance().get(type);
        if (def == null) { revalidate(); repaint(); return; }

        for (GivenField field : def.getFields().values()) {
            Object value = paramMap.get(field.name);
            if (value == null && field.defaultValue != null) value = field.defaultValue;

            // Code-Feld: Label in voller Breite + Editor in voller Breite
            if (field.type == Code.class) {
                gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
                gbc.weightx = 1.0; gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
                dynamicFieldsPanel.add(new JLabel(field.label), gbc);

                RSyntaxTextArea editor = new RSyntaxTextArea(10, 40);
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                editor.setCodeFoldingEnabled(true);
                editor.setText(value != null ? value.toString() : "");
                RTextScrollPane scrollPane = new RTextScrollPane(editor);

                gbc.gridy = ++row; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
                dynamicFieldsPanel.add(scrollPane, gbc);

                inputs.put(field.name, editor);
                row++;
                continue;
            }

            // Spezialfall: preconditionRef.id -> Dropdown statt Textfeld
            if (TYPE_PRECONDITION_REF.equals(def.getType()) && "id".equals(field.name)) {
                // Label
                gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
                gbc.weightx = 0; gbc.weighty = 0; gbc.fill = GridBagConstraints.NONE;
                dynamicFieldsPanel.add(new JLabel(field.label + ":"), gbc);

                // Combo
                gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
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
                    if (value != null && value.equals(id)) selected = item;
                }
                if (selected != null) preBox.setSelectedItem(selected);

                dynamicFieldsPanel.add(preBox, gbc);
                inputs.put(field.name, preBox);
                row++;
                continue;
            }

            // Standard: Textfeld
            gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
            gbc.weightx = 0; gbc.weighty = 0; gbc.fill = GridBagConstraints.NONE;
            dynamicFieldsPanel.add(new JLabel(field.label + ":"), gbc);

            gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
            JTextField tf = new JTextField(value != null ? value.toString() : "");
            dynamicFieldsPanel.add(tf, gbc);
            inputs.put(field.name, tf);
            row++;
        }

        revalidate();
        repaint();
    }

    private void save(ActionEvent e) {
        String selectedType = (String) typeBox.getSelectedItem();
        condition.setType(selectedType);

        Map<String, String> result = new LinkedHashMap<String, String>();

        // username sichern
        Object uSel = (userBox != null) ? userBox.getSelectedItem() : null;
        if (uSel != null && uSel.toString().trim().length() > 0) {
            result.put("username", uSel.toString().trim());
        }

        // dynamische Felder sichern
        for (Map.Entry<String, JComponent> entry : inputs.entrySet()) {
            String name = entry.getKey();
            JComponent input = entry.getValue();

            if (input instanceof JTextField) {
                result.put(name, ((JTextField) input).getText());
            } else if (input instanceof RSyntaxTextArea) {
                result.put(name, ((RSyntaxTextArea) input).getText());
            } else if (input instanceof JComboBox) {
                Object sel = ((JComboBox<?>) input).getSelectedItem();
                if (sel instanceof PreItem) {
                    result.put(name, ((PreItem) sel).id); // nur UUID speichern
                } else if (sel != null) {
                    result.put(name, sel.toString());
                }
            }
        }

        condition.setValue(serializeValueMap(result));
        TestRegistry.getInstance().save();
        JOptionPane.showMessageDialog(this, "Änderungen gespeichert.");
    }

    // ----- Helpers -----

    private GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        return gbc;
    }

    private Map<String, Object> parseValueMap(String value) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (value != null && value.contains("=")) {
            String[] pairs = value.split("&");
            for (int i = 0; i < pairs.length; i++) {
                String[] kv = pairs[i].split("=", 2);
                if (kv.length == 2) result.put(kv[0], kv[1]);
            }
        }
        return result;
    }

    private String serializeValueMap(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

    /** Anzeige: "Name {UUID}" – gespeichert wird aber NUR die UUID. */
    private static final class PreItem {
        final String id;
        final String name;
        PreItem(String id, String name) { this.id = id; this.name = name; }
        public String toString() { return name + " {" + id + "}"; }
    }
}
