package de.bund.zrb.ui.expressions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dropdown-artige Komponente für die Auswahl einer Scope-Referenz.
 *
 * Darstellung:
 *  - normale Variablen (from beforeEach / case.given): "username"
 *  - einmalige Variablen (from beforeAll):            "①sessionId"  (optional, falls befüllt)
 *  - Templates (lazy):                                "*otpCode"
 *
 * Es wird genau der sichtbare String (inkl. Präfix) als chosenName gespeichert.
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

    private String chosenName;
    private final List<SelectionListener> listeners = new ArrayList<SelectionListener>();

    // Keep last provided scope to allow lookups by caller (e.g., ActionEditorTab)
    private GivenLookupService.ScopeData currentScopeData;

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
     * Füllt die Liste anhand der gelieferten ScopeData.
     * Reihenfolge:
     *  1. variables (ohne Präfix)
     *  2. templates (mit Präfix *)
     */
    public void setScopeData(GivenLookupService.ScopeData data) {
        this.currentScopeData = data;

        listModel.clear();
        chosenName = null;
        displayField.setText("");

        if (data != null) {
            // 1) Variablen
            for (String varName : data.variables.keySet()) {
                if (varName != null) {
                    String t = varName.trim();
                    if (t.length() > 0) listModel.addElement(t);
                }
            }
            // 2) Templates (mit "*")
            for (String tmplName : data.templates.keySet()) {
                if (tmplName != null) {
                    String t = tmplName.trim();
                    if (t.length() > 0) listModel.addElement("*" + t);
                }
            }
        }
    }

    /** Provide caller access to the last scope data for lookups. */
    public GivenLookupService.ScopeData getCurrentScopeData() {
        return currentScopeData;
    }

    /**
     * Stellt die bereits gespeicherte Auswahl sichtbar, ohne Listener zu feuern.
     */
    public void setInitialChoiceWithoutEvent(String preselectName) {
        if (preselectName == null) return;
        chosenName = preselectName;
        displayField.setText(preselectName);
    }

    public void addSelectionListener(SelectionListener l) {
        if (l != null) listeners.add(l);
    }

    public String getChosenName() {
        return chosenName;
    }

    private void fireSelection(String nameWithPrefix) {
        chosenName = nameWithPrefix;
        displayField.setText(nameWithPrefix != null ? nameWithPrefix : "");
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onSelected(nameWithPrefix);
        }
    }

    private void wireInteractions() {
        dropButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { showPopup(); }
        });

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

    @SuppressWarnings("unused")
    private void addIfNotPresent(String value) {
        for (int i = 0; i < listModel.size(); i++) {
            if (value.equals(listModel.get(i))) return;
        }
        listModel.addElement(value);
    }
}
