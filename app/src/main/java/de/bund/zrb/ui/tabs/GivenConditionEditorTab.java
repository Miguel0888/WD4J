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
 * EIN Editor für Given:
 * - oben: User-Dropdown (schreibt "username" in value)
 * - Felder je Given-Typ aus GivenRegistry
 * - Spezialfall preconditionRef: Feld "id" als Precondition-Dropdown (Name {UUID}), gespeichert wird nur die UUID
 */
public class GivenConditionEditorTab extends JPanel {

    private static final String TYPE_PRECONDITION_REF = "preconditionRef";

    private final GivenCondition condition;
    private final JComboBox<String> typeBox;
    private final JComboBox<String> userBox;

    private final JPanel dynamicFieldsPanel = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> inputs = new LinkedHashMap<>();

    public GivenConditionEditorTab(GivenCondition condition) {
        this.condition = condition;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header: Typ + User
        JPanel header = new JPanel(new GridLayout(0, 2, 8, 8));

        header.add(new JLabel("Typ:"));
        String[] types = GivenRegistry.getInstance().getAll()
                .stream().map(GivenTypeDefinition::getType).toArray(String[]::new);
        typeBox = new JComboBox<>(types);
        typeBox.setEditable(true);
        typeBox.setSelectedItem(condition.getType());
        header.add(typeBox);

        header.add(new JLabel("User:"));
        String[] users = UserRegistry.getInstance().getAll().stream()
                .map(User::getUsername).toArray(String[]::new);
        userBox = new JComboBox<>(users);
        String initialUser = (String) parseValueMap(condition.getValue()).get("username");
        if (initialUser != null && !initialUser.trim().isEmpty()) {
            userBox.setSelectedItem(initialUser.trim());
        }
        header.add(userBox);

        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.add(header, BorderLayout.NORTH);
        form.add(dynamicFieldsPanel, BorderLayout.CENTER);
        add(form, BorderLayout.CENTER);

        typeBox.addActionListener(e -> rebuildDynamicForm(String.valueOf(typeBox.getSelectedItem())));

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

        GivenTypeDefinition def = GivenRegistry.getInstance().get(type);
        if (def == null) { revalidate(); repaint(); return; }

        Map<String, Object> paramMap = parseValueMap(condition.getValue());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        int row = 0;
        for (GivenField field : def.getFields().values()) {
            Object value = paramMap.getOrDefault(field.name, field.defaultValue);

            // Code-Editor
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

            // Spezialfall: preconditionRef.id -> Dropdown
            if (TYPE_PRECONDITION_REF.equals(def.getType()) && "id".equals(field.name)) {
                // Label
                gbc.gridx = 0; gbc.gridy = row;
                gbc.gridwidth = 1; gbc.weightx = 0; gbc.weighty = 0; gbc.fill = GridBagConstraints.NONE;
                dynamicFieldsPanel.add(new JLabel(field.label + ":"), gbc);

                // Combo mit Name {UUID}
                gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
                JComboBox<PreItem> preBox = new JComboBox<>();
                List<Precondition> pres = PreconditionRegistry.getInstance().getAll();
                PreItem selected = null;
                for (Precondition p : pres) {
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
            gbc.gridx = 0; gbc.gridy = row;
            gbc.gridwidth = 1; gbc.weightx = 0; gbc.weighty = 0; gbc.fill = GridBagConstraints.NONE;
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
        // Typ speichern
        condition.setType(String.valueOf(typeBox.getSelectedItem()));

        Map<String, String> result = new LinkedHashMap<>();

        // User speichern (wie im When-Tab)
        Object u = userBox.getSelectedItem();
        if (u != null && u.toString().trim().length() > 0) {
            result.put("username", u.toString().trim());
        }

        // Dynamische Felder speichern
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
                    result.put(name, ((PreItem) sel).id); // NUR UUID speichern
                } else if (sel != null) {
                    result.put(name, sel.toString());
                }
            }
        }

        condition.setValue(serializeValueMap(result));
        TestRegistry.getInstance().save();
        JOptionPane.showMessageDialog(this, "Änderungen gespeichert.");
    }

    // Helpers

    private Map<String, Object> parseValueMap(String value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value != null && value.contains("=")) {
            String[] pairs = value.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
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

    /** Anzeige: "Name {UUID}" – gespeichert wird nur die UUID. */
    private static final class PreItem {
        final String id;
        final String name;
        PreItem(String id, String name) { this.id = id; this.name = name; }
        public String toString() { return name + " {" + id + "}"; }
    }
}
