package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.GivenRegistry;
import de.bund.zrb.model.GivenTypeDefinition;
import de.bund.zrb.model.GivenTypeDefinition.GivenField;
import de.bund.zrb.model.Precondition;
import de.bund.zrb.service.PreconditionRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.parts.Code;
import de.bund.zrb.ui.parts.PreconditionRef;  // <— neu

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GivenConditionEditorTab extends JPanel {

    private static final String KEY_ID = "id";

    private final GivenCondition condition;
    private final JComboBox<String> typeBox;
    private final JPanel dynamicFieldsPanel = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> inputs = new LinkedHashMap<String, JComponent>();

    public GivenConditionEditorTab(GivenCondition condition) {
        this.condition = condition;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        typeBox = new JComboBox<String>(GivenRegistry.getInstance().getAll()
                .stream().map(GivenTypeDefinition::getType).toArray(String[]::new));
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

        GivenTypeDefinition def = GivenRegistry.getInstance().get(type);
        if (def == null) { revalidate(); repaint(); return; }

        Map<String, Object> paramMap = parseValueMap(condition.getValue());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        int row = 0;

        for (GivenField field : def.getFields().values()) {
            Object value = paramMap.containsKey(field.name) ? paramMap.get(field.name) : field.defaultValue;

            // Label
            gbc.gridx = 0; gbc.gridy = row;
            gbc.gridwidth = 1; gbc.weightx = 0; gbc.weighty = 0;
            gbc.fill = GridBagConstraints.NONE;
            dynamicFieldsPanel.add(new JLabel(field.label + ":"), gbc);

            // Input
            gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;

            JComponent input;

            // --- Spezieller Editor für PreconditionRef-Felder ---
            if (field.type == PreconditionRef.class && KEY_ID.equals(field.name)) {
                JPanel idRow = new JPanel(new BorderLayout(6, 0));

                // Namen (→ UUID) anbieten
                JComboBox<Item> nameBox = new JComboBox<Item>(buildPreconditionItems());
                String currentId = stringOrEmpty(paramMap.get(KEY_ID));
                preselectById(nameBox, currentId);

                // UUID-Feld geschützt, per Stift editierbar
                JTextField idField = new JTextField(currentId);
                idField.setEditable(false);
                idField.setEnabled(true);
                idField.setBackground(UIManager.getColor("TextField.inactiveBackground"));

                JButton pencil = new JButton("✎");
                pencil.setToolTipText("ID ändern");
                Dimension d = new Dimension(28, idField.getPreferredSize().height);
                pencil.setPreferredSize(d);
                pencil.setFocusable(false);
                pencil.addActionListener(new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        boolean makeEditable = !idField.isEditable();
                        idField.setEditable(makeEditable);
                        idField.setBackground(makeEditable
                                ? UIManager.getColor("TextField.background")
                                : UIManager.getColor("TextField.inactiveBackground"));
                        if (makeEditable) {
                            idField.requestFocusInWindow();
                            idField.selectAll();
                        }
                    }
                });

                nameBox.addActionListener(new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        Item it = (Item) nameBox.getSelectedItem();
                        if (it != null && it.uuid != null && it.uuid.length() > 0) {
                            idField.setText(it.uuid);
                        }
                    }
                });

                idRow.add(nameBox, BorderLayout.WEST);
                idRow.add(idField, BorderLayout.CENTER);
                idRow.add(pencil, BorderLayout.EAST);

                input = idRow;
                inputs.put("__precond_nameBox__", nameBox);
                inputs.put(field.name, idField);
            }
            // Code-Editor
            else if (field.type == Code.class) {
                RSyntaxTextArea editor = new RSyntaxTextArea(10, 40);
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                editor.setCodeFoldingEnabled(true);
                editor.setText(value != null ? String.valueOf(value) : "");

                RTextScrollPane scrollPane = new RTextScrollPane(editor);

                gbc.gridx = 0; gbc.gridy = row++;
                gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                dynamicFieldsPanel.add(new JLabel(field.label), gbc);

                gbc.gridx = 0; gbc.gridy = row++;
                gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 1.0;
                gbc.fill = GridBagConstraints.BOTH;
                dynamicFieldsPanel.add(scrollPane, gbc);

                inputs.put(field.name, editor);
                continue;
            }
            // Standard Textfeld
            else {
                JTextField tf = new JTextField(value != null ? String.valueOf(value) : "");
                input = tf;
                inputs.put(field.name, tf);
            }

            dynamicFieldsPanel.add(input, gbc);
            row++;
        }

        revalidate();
        repaint();
    }

    private void save(ActionEvent e) {
        String selectedType = (String) typeBox.getSelectedItem();
        condition.setType(selectedType);

        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, JComponent> entry : inputs.entrySet()) {
            String name = entry.getKey();
            if ("__precond_nameBox__".equals(name)) continue; // nur Helper

            JComponent comp = entry.getValue();
            if (comp instanceof JTextField) {
                result.put(name, ((JTextField) comp).getText());
            } else if (comp instanceof RSyntaxTextArea) {
                result.put(name, ((RSyntaxTextArea) comp).getText());
            } else if (comp instanceof JComboBox) {
                Object sel = ((JComboBox<?>) comp).getSelectedItem();
                result.put(name, sel != null ? String.valueOf(sel) : "");
            }
        }

        condition.setValue(serializeValueMap(result));
        TestRegistry.getInstance().save();
        JOptionPane.showMessageDialog(this, "Änderungen gespeichert.");
    }

    // -------- Helpers --------

    private static final class Item {
        final String name;
        final String uuid;
        Item(String name, String uuid) { this.name = name; this.uuid = uuid; }
        public String toString() {
            return (name != null && name.trim().length() > 0) ? name.trim() : "(unnamed)";
        }
    }

    private Item[] buildPreconditionItems() {
        List<Precondition> all = PreconditionRegistry.getInstance().getAll();
        Item[] arr = new Item[all.size()];
        for (int i = 0; i < all.size(); i++) {
            Precondition p = all.get(i);
            String nm = (p.getName() != null && p.getName().trim().length() > 0) ? p.getName().trim() : "(unnamed)";
            arr[i] = new Item(nm, p.getId());
        }
        return arr;
    }

    private void preselectById(JComboBox<Item> box, String id) {
        if (id == null || id.trim().length() == 0) return;
        for (int i = 0; i < box.getItemCount(); i++) {
            Item it = box.getItemAt(i);
            if (id.equals(it.uuid)) { box.setSelectedIndex(i); return; }
        }
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

    private String stringOrEmpty(Object v) { return v == null ? "" : String.valueOf(v); }
}
