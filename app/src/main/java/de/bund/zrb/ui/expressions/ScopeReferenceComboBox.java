package de.bund.zrb.ui.expressions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UI-"Dropdown" für Variablen-/Template-Referenzen.
 *
 * Anzeigeprinzip:
 *   username
 *   belegnummer
 *   ①globalSessionId      (BeforeAll -> läuft nur einmal)
 *   *otpCode              (Template -> lazy im WHEN)
 *
 * chosenName speichert GENAU den sichtbaren Namen inkl. Präfix.
 */
public class ScopeReferenceComboBox extends JPanel {

    public interface SelectionListener {
        void onSelected(String nameWithPrefix);
    }

    private final JTextField displayField;
    private final JButton dropButton;
    private final JPopupMenu popup;
    private final JList<String> list;
    private final DefaultListModel<String> listModel;

    private String chosenName; // inkl. Präfix falls vorhanden
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
        scroll.setPreferredSize(new Dimension(220, 160));

        popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        popup.add(scroll, BorderLayout.CENTER);

        add(displayField, BorderLayout.CENTER);
        add(dropButton, BorderLayout.EAST);

        wireInteractions();
    }

    /**
     * ScopeData -> baue Liste.
     *
     * Reihenfolge:
     *   (1) beforeEachVars (kein Präfix)
     *   (2) beforeAllVars  (mit "①")
     *   (3) templates      (mit "*")
     *
     * Shadowing war schon vorher in GivenLookupService erledigt,
     * wir bekommen hier also die "gewinnt schon"-Namen.
     */
    public void setScopeData(GivenLookupService.ScopeData data) {
        listModel.clear();
        chosenName = null;
        displayField.setText("");

        if (data == null) {
            return;
        }

        // 1. normale Variablen aus beforeEach
        for (String varName : data.beforeEachNames) {
            addIfNotPresent(listModel, varName);
        }

        // 2. beforeAll-Variablen mit Präfix "①"
        for (String varName : data.beforeAllNames) {
            String decorated = "①" + varName;
            addIfNotPresent(listModel, decorated);
        }

        // 3. Templates mit Präfix "*"
        for (String tmplName : data.templateNames) {
            String decorated = "*" + tmplName;
            addIfNotPresent(listModel, decorated);
        }
    }

    private void addIfNotPresent(DefaultListModel<String> model, String value) {
        for (int i = 0; i < model.size(); i++) {
            if (value.equals(model.get(i))) {
                return;
            }
        }
        model.addElement(value);
    }

    public void addSelectionListener(SelectionListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    /**
     * Gibt den gewählten Wert zurück, inkl. evtl. Präfix.
     * Also z.B. "username", "①globalSessionId" oder "*otpCode".
     */
    public String getChosenName() {
        return chosenName;
    }

    /**
     * Falls wir eine Action erneut öffnen, können wir dem User den
     * alten gespeicherten Namen anzeigen (statt leerer Box).
     * Das hier kannst du aus ActionEditorTab aufrufen.
     */
    public void presetSelection(String previousNameWithPrefix) {
        chosenName = previousNameWithPrefix;
        displayField.setText(previousNameWithPrefix != null ? previousNameWithPrefix : "");
    }

    private void fireSelection(String nameWithPrefix) {
        chosenName = nameWithPrefix;
        displayField.setText(nameWithPrefix != null ? nameWithPrefix : "");

        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onSelected(nameWithPrefix);
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
    }

    private void showPopup() {
        popup.show(this, 0, this.getHeight());

        // Für schnelles ENTER direkt was auswählen
        if (list.getModel().getSize() > 0 && list.getSelectedIndex() < 0) {
            list.setSelectedIndex(0);
        }
        list.requestFocusInWindow();
    }

    private void hidePopup() {
        popup.setVisible(false);
    }

    private void confirmCurrentSelection() {
        String sel = list.getSelectedValue();
        if (sel != null) {
            fireSelection(sel);
        }
        hidePopup();
    }
}
