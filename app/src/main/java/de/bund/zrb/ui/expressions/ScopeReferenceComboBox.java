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
 *  - einmalige Variablen (from beforeAll):            "①sessionId"
 *  - Templates (lazy):                                "*otpCode"
 *
 * Wir speichern genau den sichtbaren String (inkl. Präfix) als chosenName.
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
    private final List<SelectionListener> listeners = new ArrayList<>();

    public ScopeReferenceComboBox() {
        super(new BorderLayout());

        displayField = new JTextField();
        displayField.setEditable(false);

        dropButton = new JButton("▼");
        dropButton.setMargin(new Insets(0, 4, 0, 4));
        dropButton.setFocusable(false);

        listModel = new DefaultListModel<>();
        list = new JList<>(listModel);
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
     *  1. beforeEachNames  (ohne Präfix)
     *  2. beforeAllNames   (mit Präfix ①)
     *  3. templateNames    (mit Präfix *)
     */
    public void setScopeData(GivenLookupService.ScopeData data) {
        listModel.clear();
        chosenName = null;
        displayField.setText("");

        if (data == null) {
            return;
        }

        // normale Variablen
        for (String varName : data.beforeEachNames) {
            addIfNotPresent(varName);
        }

        // einmalige Variablen (BeforeAll)
        for (String varName : data.beforeAllNames) {
            addIfNotPresent("①" + varName);
        }

        // Templates
        for (String tmplName : data.templateNames) {
            addIfNotPresent("*" + tmplName);
        }
    }

    /**
     * Wird vom ActionEditorTab genutzt, um beim Öffnen die bereits gespeicherte
     * Auswahl wieder sichtbar zu machen, OHNE als neue Auswahl zu feuern.
     *
     * Beispiel:
     *  - Action.value == "{{username}}"          -> preselectName = "username"
     *  - Action.value == "{{otpCode()}}"         -> preselectName = "*otpCode"
     *  - Action.value == "{{sessionId}}" die aus beforeAll stammt -> "①sessionId"
     *
     * Wichtig: Du baust preselectName vorher selbst (deriveScopeNameFromTemplate).
     */
    public void setInitialChoiceWithoutEvent(String preselectName) {
        if (preselectName == null) return;
        chosenName = preselectName;
        displayField.setText(preselectName);
    }

    public void addSelectionListener(SelectionListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    public String getChosenName() {
        return chosenName;
    }

    private void fireSelection(String nameWithPrefix) {
        chosenName = nameWithPrefix;
        displayField.setText(nameWithPrefix != null ? nameWithPrefix : "");

        for (SelectionListener l : listeners) {
            l.onSelected(nameWithPrefix);
        }
    }

    private void wireInteractions() {
        dropButton.addActionListener(e -> showPopup());

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

    private void addIfNotPresent(String value) {
        for (int i = 0; i < listModel.size(); i++) {
            if (value.equals(listModel.get(i))) {
                return;
            }
        }
        listModel.addElement(value);
    }
}
