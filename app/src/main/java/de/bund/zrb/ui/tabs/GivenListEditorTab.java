package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.Precondition;
import de.bund.zrb.service.PreconditionRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.dialogs.GivenChoiceDialog;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Edit a list of GivenCondition objects:
 * - Offer all Given types and all Preconditions in a single combo via GivenChoiceDialog
 * - Allow multiple items and reorder (Up/Down)
 * - For standard Given, open GivenConditionEditorTab on "Edit"
 * - For precondition reference, allow to re-pick another precondition
 */
public class GivenListEditorTab extends JPanel {

    private static final String TYPE_PRECONDITION_REF = "preconditionRef"; // agreed convention

    private final List<GivenCondition> model; // the live list from suite/case
    private final DefaultListModel<GivenCondition> listModel = new DefaultListModel<GivenCondition>();
    private final JList<GivenCondition> list;

    public GivenListEditorTab(List<GivenCondition> givenList) {
        super(new BorderLayout(10, 10));
        this.model = (givenList != null) ? givenList : new ArrayList<GivenCondition>();
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        list = new JList<GivenCondition>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new GivenCellRenderer());

        // Fill from model
        for (int i = 0; i < this.model.size(); i++) {
            listModel.addElement(this.model.get(i));
        }

        add(new JScrollPane(list), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.EAST);
    }

    private JComponent buildButtons() {
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(0, 1, 6, 6));

        JButton add = new JButton("Hinzufügen");
        add.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                onAdd();
            }
        });

        JButton edit = new JButton("Bearbeiten");
        edit.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                onEdit();
            }
        });

        JButton del = new JButton("Löschen");
        del.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                onDelete();
            }
        });

        JButton up = new JButton("▲");
        up.setToolTipText("Nach oben");
        up.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                onMove(-1);
            }
        });

        JButton down = new JButton("▼");
        down.setToolTipText("Nach unten");
        down.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                onMove(1);
            }
        });

        JButton save = new JButton("Speichern");
        save.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                TestRegistry.getInstance().save();
                JOptionPane.showMessageDialog(GivenListEditorTab.this, "Änderungen gespeichert.");
            }
        });

        p.add(add);
        p.add(edit);
        p.add(del);
        p.add(up);
        p.add(down);
        p.add(save);

        return p;
    }

    // -------------------- Actions --------------------

    private void onAdd() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        GivenChoiceDialog dlg = new GivenChoiceDialog(owner, "Given hinzufügen", null);
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) return;

        int kind = dlg.getSelectedKind();
        String idOrType = dlg.getIdOrType();

        // We only support picking an existing Precondition (or none).
        // So we always create a GivenCondition of type "preconditionRef".

        GivenCondition gc = new GivenCondition();
        gc.setType("preconditionRef"); // or TYPE_PRECONDITION_REF constant
        gc.setValue("id=" + idOrType); // idOrType is the UUID (or "" if user chose the empty entry)

        model.add(gc);
        listModel.addElement(gc);
        list.setSelectedValue(gc, true);

        // Optional: direkt Editor öffnen, wenn du das alte Verhalten behalten willst
        openEditorFor(gc);

    }

    private void onEdit() {
        GivenCondition sel = list.getSelectedValue();
        if (sel == null) return;

        if (TYPE_PRECONDITION_REF.equals(sel.getType())) {
            // Re-pick the precondition
            String currentId = parseIdFromValue(sel.getValue());
            Window owner = SwingUtilities.getWindowAncestor(this);
            GivenChoiceDialog dlg = new GivenChoiceDialog(owner, "Precondition wählen", currentId);
            dlg.setVisible(true);
            if (!dlg.isConfirmed()) return;
            if (dlg.getSelectedKind() != GivenChoiceDialog.KIND_PRECONDITION) return;

            String newId = dlg.getIdOrType();
            if (newId != null && newId.length() > 0 && !newId.equals(currentId)) {
                sel.setValue("id=" + newId);
                list.repaint();
            }
        } else {
            // Standard Given → open rich editor tab
            openEditorFor(sel);
        }
    }

    private void onDelete() {
        GivenCondition sel = list.getSelectedValue();
        if (sel == null) return;
        int idx = list.getSelectedIndex();
        model.remove(sel);
        listModel.removeElement(sel);
        if (!listModel.isEmpty()) {
            int nidx = Math.max(0, Math.min(idx, listModel.size() - 1));
            list.setSelectedIndex(nidx);
        }
    }

    private void onMove(int delta) {
        int idx = list.getSelectedIndex();
        if (idx < 0) return;

        int nidx = idx + delta;
        if (nidx < 0 || nidx >= listModel.size()) return;

        // Swap in model
        GivenCondition a = model.get(idx);
        GivenCondition b = model.get(nidx);
        model.set(idx, b);
        model.set(nidx, a);

        // Swap in list model
        listModel.set(idx, b);
        listModel.set(nidx, a);

        list.setSelectedIndex(nidx);
        list.ensureIndexIsVisible(nidx);
    }

    // -------------------- Helpers --------------------

    private void openEditorFor(GivenCondition given) {
        // Find a top-level tabbed pane and open GivenConditionEditorTab
        Component parent = SwingUtilities.getWindowAncestor(this);
        if (parent instanceof JFrame) {
            JTabbedPane tabs = UIHelper.findTabbedPane((JFrame) parent);
            if (tabs != null) {
                GivenConditionEditorTab tab = new GivenConditionEditorTab(given);
                tabs.addTab("Given: " + given.getType(), tab);
                tabs.setSelectedComponent(tab);
            }
        }
    }

    private String parseIdFromValue(String value) {
        if (value == null) return "";
        String[] parts = value.split("&");
        for (int i = 0; i < parts.length; i++) {
            String[] kv = parts[i].split("=", 2);
            if (kv.length == 2 && "id".equals(kv[0])) return kv[1];
        }
        return "";
    }

    // -------------------- Renderer --------------------

    private static class GivenCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof GivenCondition) {
                GivenCondition gc = (GivenCondition) value;
                if ("preconditionRef".equals(gc.getType())) {
                    String id = "";
                    String v = gc.getValue();
                    if (v != null) {
                        String[] parts = v.split("&");
                        for (int i = 0; i < parts.length; i++) {
                            String[] kv = parts[i].split("=", 2);
                            if (kv.length == 2 && "id".equals(kv[0])) { id = kv[1]; break; }
                        }
                    }
                    // Resolve name for display
                    String name = id;
                    List<Precondition> pres = PreconditionRegistry.getInstance().getAll();
                    for (int i = 0; i < pres.size(); i++) {
                        Precondition p = pres.get(i);
                        if (id.equals(p.getId())) {
                            name = (p.getName() != null && p.getName().trim().length() > 0)
                                    ? p.getName().trim() : "(unnamed)";
                            break;
                        }
                    }
                    c.setText("Precondition: " + name + " {" + id + "}");
                } else {
                    c.setText("Given: " + gc.getType());
                }
            }
            return c;
        }
    }
}
