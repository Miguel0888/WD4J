package de.bund.zrb.ui.expressions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Eine "ComboBox-artige" Komponente, aber mit eigener JList in einem Popup.
 *
 * Anzeige:
 *  - TextField (nicht editierbar) mit aktuell gewähltem Eintrag
 *  - kleiner Button "▼" zum Aufklappen
 *
 * Popup:
 *  - JList<String> mit allen möglichen Referenzen.
 *    Reihenfolge:
 *      1. Variablen   (z.B. username)
 *      2. Templates   (als *otpCode, *wrapText, ...)
 *
 * Auswahl:
 *  - Doppelklick oder ENTER auf der Liste bestätigt und schließt.
 *
 * Verwenden im ActionEditorTab:
 *  - combo.setScopeData(scopeDataFromLookup);
 *  - combo.addSelectionListener(name -> { ... });
 *  - combo.getChosenName();
 */
public class ScopeReferenceComboBox extends JPanel {

    public interface SelectionListener {
        void onSelected(String name);
    }

    private final JTextField displayField;
    private final JButton dropButton;
    private final JPopupMenu popup;
    private final JList<String> list;
    private final DefaultListModel<String> listModel;

    private String chosenName;
    private final List<SelectionListener> listeners = new ArrayList<SelectionListener>();

    public ScopeReferenceComboBox() {
        super(new BorderLayout());

        displayField = new JTextField();
        displayField.setEditable(false);

        dropButton = new JButton("▼");
        dropButton.setMargin(new Insets(0, 4, 0, 4));
        dropButton.setFocusable(false);

        listModel = new DefaultListModel<String>();
        list = new JList<String>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(200, 150));

        popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        popup.add(scroll, BorderLayout.CENTER);

        add(displayField, BorderLayout.CENTER);
        add(dropButton, BorderLayout.EAST);

        wireInteractions();
    }

    /**
     * Übergib neue ScopeData (Variablen + Templates).
     * Wir bauen daraus das Listenmodell neu auf.
     */
    public void setScopeData(GivenLookupService.ScopeData data) {
        listModel.clear();
        chosenName = null;
        displayField.setText("");

        if (data != null) {
            // 1. Variablen
            Iterator<String> itVars = data.variables.iterator();
            while (itVars.hasNext()) {
                String varName = itVars.next();
                if (varName != null && varName.trim().length() > 0) {
                    listModel.addElement(varName.trim());
                }
            }

            // 2. Templates (mit führendem "*")
            Iterator<String> itTmpl = data.templates.iterator();
            while (itTmpl.hasNext()) {
                String tName = itTmpl.next();
                if (tName != null && tName.trim().length() > 0) {
                    listModel.addElement("*" + tName.trim());
                }
            }
        }
    }

    public void addSelectionListener(SelectionListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    public String getChosenName() {
        return chosenName;
    }

    private void fireSelection(String name) {
        chosenName = name;
        displayField.setText(name != null ? name : "");

        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onSelected(name);
        }
    }

    private void wireInteractions() {
        // Button klappt Popup auf
        dropButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPopup();
            }
        });

        // ENTER bestätigt Auswahl
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (KeyEvent.VK_ENTER == e.getKeyCode()) {
                    confirmCurrentSelection();
                } else if (KeyEvent.VK_ESCAPE == e.getKeyCode()) {
                    hidePopup();
                }
            }
        });

        // Doppelklick bestätigt Auswahl
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    confirmCurrentSelection();
                }
            }
        });

        // Wenn das Textfeld den Fokus verliert -> Popup bleibt erstmal, ist okay.
        displayField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // UX-Feinschliff wäre möglich.
            }
        });
    }

    private void showPopup() {
        popup.show(this, 0, this.getHeight());

        // fürs schnelle ENTER schon mal was auswählen
        if (list.getModel().getSize() > 0 && list.getSelectedIndex() < 0) {
            list.setSelectedIndex(0);
        }
        list.requestFocusInWindow();
    }

    private void hidePopup() {
        popup.setVisible(false);
    }

    /**
     * Holt den aktuell selektierten Eintrag aus der Liste,
     * übernimmt ihn als chosenName und feuert Listener.
     */
    private void confirmCurrentSelection() {
        String sel = list.getSelectedValue();
        if (sel != null) {
            fireSelection(sel);
        }
        hidePopup();
    }
}
