package de.bund.zrb.ui.dialogs;

import de.bund.zrb.model.GivenRegistry;
import de.bund.zrb.model.GivenTypeDefinition;
import de.bund.zrb.model.Precondition;
import de.bund.zrb.service.PreconditionRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/** Show a modal dialog that lets the user pick either a Given type or a Precondition reference. */
public class GivenChoiceDialog extends JDialog {

    public static final int KIND_GIVEN_TYPE = 1;
    public static final int KIND_PRECONDITION = 2;

    private final JComboBox<Item> combo;
    private boolean confirmed;

    /** Wrap a display string plus kind and id (for precondition). */
    private static class Item {
        final String display;
        final int kind;
        final String idOrType; // for GIVEN_TYPE: the given type; for PRECONDITION: the precondition UUID

        Item(String display, int kind, String idOrType) {
            this.display = display;
            this.kind = kind;
            this.idOrType = idOrType;
        }

        public String toString() {
            return display;
        }
    }

    public GivenChoiceDialog(Window owner, String title, String preselectPreconditionIdOrType) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        combo = new JComboBox<Item>(buildItems());
        if (preselectPreconditionIdOrType != null && preselectPreconditionIdOrType.trim().length() > 0) {
            preselect(preselectPreconditionIdOrType.trim());
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        form.add(new JLabel("Given/Precondition:"));
        form.add(combo);

        JButton ok = new JButton("OK");
        ok.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });

        JButton cancel = new JButton("Abbrechen");
        cancel.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        getRootPane().setDefaultButton(ok);
        getRootPane().registerKeyboardAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        }, KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(ok);
        south.add(cancel);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(south, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private Item[] buildItems() {
        List<Item> items = new ArrayList<Item>();

        // Add all Given types
        GivenTypeDefinition[] defs = GivenRegistry.getInstance().getAll().toArray(new GivenTypeDefinition[0]);
        for (int i = 0; i < defs.length; i++) {
            GivenTypeDefinition def = defs[i];
            String display = "Given: " + def.getType();
            items.add(new Item(display, KIND_GIVEN_TYPE, def.getType()));
        }

        // Add all Preconditions
        List<Precondition> pres = PreconditionRegistry.getInstance().getAll();
        for (int i = 0; i < pres.size(); i++) {
            Precondition p = pres.get(i);
            String name = (p.getName() != null && p.getName().trim().length() > 0) ? p.getName().trim() : "(unnamed)";
            String display = "Precondition: " + name + "  {" + p.getId() + "}";
            items.add(new Item(display, KIND_PRECONDITION, p.getId()));
        }
        return items.toArray(new Item[0]);
    }

    private void preselect(String idOrType) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Item it = (Item) combo.getItemAt(i);
            if (idOrType.equals(it.idOrType)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /** Return true if user confirmed with OK. */
    public boolean isConfirmed() {
        return confirmed;
    }

    /** Return selected kind (GIVEN_TYPE or PRECONDITION). */
    public int getSelectedKind() {
        Item it = (Item) combo.getSelectedItem();
        return (it == null) ? 0 : it.kind;
    }

    /** Return selected id or type depending on kind. */
    public String getIdOrType() {
        Item it = (Item) combo.getSelectedItem();
        return (it == null) ? "" : it.idOrType;
    }
}
