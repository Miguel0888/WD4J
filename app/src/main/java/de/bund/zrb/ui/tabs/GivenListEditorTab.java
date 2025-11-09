package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.Precondtion;
import de.bund.zrb.model.Precondition;
import de.bund.zrb.service.PreconditionRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.dialogs.GivenChoiceDialog;

import javax.swing.*;
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

    private final List<Precondtion> model; // the live list from suite/case
    private final DefaultListModel<Precondtion> listModel = new DefaultListModel<Precondtion>();
    private final JList<Precondtion> list;
    private final JLabel contextLabel;
    private final JLabel validationLabel;

    public GivenListEditorTab(String scopeContext, List<Precondtion> givenList) {
        super(new BorderLayout(10, 10));
        this.model = (givenList != null) ? givenList : new ArrayList<Precondtion>();
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        contextLabel = new JLabel();
        contextLabel.setFont(contextLabel.getFont().deriveFont(Font.BOLD));
        validationLabel = new JLabel();
        validationLabel.setVisible(false);

        JPanel header = new JPanel(new BorderLayout(6, 6));
        header.setOpaque(false);
        header.add(contextLabel, BorderLayout.WEST);
        header.add(validationLabel, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        setScopeContext(scopeContext);
        clearValidationError();

        list = new JList<Precondtion>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new GivenCellRenderer());

        // Fill from model
        for (int i = 0; i < this.model.size(); i++) {
            listModel.addElement(this.model.get(i));
        }

        add(new JScrollPane(list), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.EAST);
    }

    public void setScopeContext(String scopeContext) {
        String text = (scopeContext == null || scopeContext.trim().isEmpty())
                ? "Preconditions"
                : scopeContext.trim();
        contextLabel.setText(text);
    }

    public void showValidationError(String message) {
        if (message == null || message.trim().isEmpty()) {
            clearValidationError();
            return;
        }
        validationLabel.setText("<html><span style='color:#b71c1c'>" + escapeHtml(message) + "</span></html>");
        validationLabel.setVisible(true);
    }

    public void clearValidationError() {
        validationLabel.setText("");
        validationLabel.setVisible(false);
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
                //DEBUG
//                JOptionPane.showMessageDialog(GivenListEditorTab.this, "Änderungen gespeichert.");
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

        Precondtion gc = new Precondtion();
        gc.setType(PreconditionListUtil.TYPE_PRECONDITION_REF);
        gc.setValue("id=" + idOrType); // idOrType is the UUID (or "" if user chose the empty entry)

        model.add(gc);
        listModel.addElement(gc);
        list.setSelectedValue(gc, true);

        // Previously: openEditorFor(gc);
        // New behavior: wenn es kein Precondition-Ref ist, öffnen wir den Editor als Dialog
        if (!PreconditionListUtil.TYPE_PRECONDITION_REF.equals(gc.getType())) {
            openEditorDialogFor(gc);
        }

    }

    private void onEdit() {
        Precondtion sel = list.getSelectedValue();
        if (sel == null) return;

        if (PreconditionListUtil.TYPE_PRECONDITION_REF.equals(sel.getType())) {
            // Re-pick the precondition
            String currentId = PreconditionListUtil.extractPreconditionId(sel);
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
            // Standard Given → open rich editor dialog (statt neuer Tab)
            openEditorDialogFor(sel);
        }
    }

    private void onDelete() {
        Precondtion sel = list.getSelectedValue();
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
        Precondtion a = model.get(idx);
        Precondtion b = model.get(nidx);
        model.set(idx, b);
        model.set(nidx, a);

        // Swap in list model
        listModel.set(idx, b);
        listModel.set(nidx, a);

        list.setSelectedIndex(nidx);
        list.ensureIndexIsVisible(nidx);
    }

    // -------------------- Helpers --------------------

    private void openEditorFor(Precondtion given) {
        // Legacy kept for callers; no-op now to avoid opening top-level tabs
    }

    /** New: open a modal dialog containing the GivenConditionEditorTab UI so the user can edit without creating a top-level tab. */
    private void openEditorDialogFor(Precondtion given) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JFrame frame = (owner instanceof JFrame) ? (JFrame) owner : null;
        JDialog dlg = new JDialog(frame, "Given bearbeiten", Dialog.ModalityType.APPLICATION_MODAL);
        GivenConditionEditorTab content = new GivenConditionEditorTab(given);
        dlg.getContentPane().setLayout(new BorderLayout());
        dlg.getContentPane().add(content, BorderLayout.CENTER);
        JButton save = new JButton("Speichern");
        save.addActionListener(e -> {
            // Trigger save on content and close dialog
            // content.save() existiert nicht öffentlich; rely on parent saving later or TestRegistry
            TestRegistry.getInstance().save();
            dlg.dispose();
        });
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(save);
        dlg.getContentPane().add(south, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }

    // -------------------- Renderer --------------------

    private static class GivenCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Precondtion) {
                Precondtion gc = (Precondtion) value;
                if (PreconditionListUtil.TYPE_PRECONDITION_REF.equals(gc.getType())) {
                    String id = PreconditionListUtil.extractPreconditionId(gc);
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

    private String escapeHtml(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }
}
