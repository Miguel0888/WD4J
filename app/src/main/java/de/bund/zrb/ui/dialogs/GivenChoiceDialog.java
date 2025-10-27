package de.bund.zrb.ui.dialogs;

import de.bund.zrb.model.Precondition;
import de.bund.zrb.service.PreconditionRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Show a modal dialog to pick a Precondition by name/UUID.
 *
 * Intent:
 * - Allow tester to insert an existing Precondition into a scenario.
 * - We do not offer generic "Given types" anymore, because GivenRegistry is gone.
 */
public class GivenChoiceDialog extends JDialog {

    public static final int KIND_PRECONDITION = 2;

    private final JComboBox<Item> combo;
    private boolean confirmed;

    /** Represent one selectable row in the combo. */
    private static class Item {
        final String display;
        final String id; // UUID of the Precondition (or "" if none)

        Item(String display, String id) {
            this.display = display;
            this.id = id;
        }

        public String toString() {
            return display;
        }
    }

    public GivenChoiceDialog(Window owner, String title, String preselectPreconditionId) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        combo = new JComboBox<Item>(buildItems());
        if (preselectPreconditionId != null && preselectPreconditionId.trim().length() > 0) {
            preselect(preselectPreconditionId.trim());
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        form.add(new JLabel("Precondition:"));
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

        // Leer-Option zuerst
        items.add(new Item("", ""));

        List<Precondition> pres = PreconditionRegistry.getInstance().getAll();
        for (int i = 0; i < pres.size(); i++) {
            Precondition p = pres.get(i);
            String id = p.getId();
            String name = (p.getName() != null && p.getName().trim().length() > 0)
                    ? p.getName().trim()
                    : "(unnamed)";
            String display = name + "  {" + id + "}";
            items.add(new Item(display, id));
        }
        return items.toArray(new Item[0]);
    }

    private void preselect(String id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Item it = (Item) combo.getItemAt(i);
            if (id.equals(it.id)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /** Return true if user pressed OK. */
    public boolean isConfirmed() {
        return confirmed;
    }

    /** Always KIND_PRECONDITION now. */
    public int getSelectedKind() {
        return KIND_PRECONDITION;
    }

    /** Return UUID of chosen Precondition (or "" if empty). */
    public String getIdOrType() {
        Item it = (Item) combo.getSelectedItem();
        return (it == null) ? "" : it.id;
    }
}
