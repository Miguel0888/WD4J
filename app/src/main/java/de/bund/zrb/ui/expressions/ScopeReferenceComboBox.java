package de.bund.zrb.ui.expressions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Eine Dropdown-ähnliche Komponente für die Auswahl von
 * Scope-Referenzen (Variablen und Templates), die in einem WHEN verwendet
 * werden dürfen.
 *
 * UI-Verhalten:
 *  - Nicht-editierbares Textfeld zeigt die aktuelle Auswahl (z. B. "username" oder "*otpCode").
 *  - Rechts daneben ein kleiner ▼-Button.
 *  - Beim Klick auf den Button geht ein Popup mit einer JList<String> auf.
 *  - Doppelklick oder ENTER auf der Liste bestätigt die Auswahl.
 *
 * Datenmodell:
 *  - setScopeData(..) bekommt eine ScopeData (Variablennamen + Templatenamen).
 *    Wir bauen daraus ein DefaultListModel<String>:
 *      erst normale Variablen (unverändert),
 *      dann Templates mit führendem "*".
 *
 * Verwendung:
 *   ScopeReferenceComboBox combo = new ScopeReferenceComboBox();
 *   combo.setScopeData(givenLookup.collectScopeFor(testCaseOrWhatever));
 *   container.add(combo, ...);
 *
 *   // Später (z.B. beim Speichern der Action):
 *   String refName = combo.getChosenName();
 *   // -> z.B. in TestAction.value übernehmen
 */
public class ScopeReferenceComboBox extends JPanel {

    private final JTextField displayField;
    private final JButton dropButton;
    private final JPopupMenu popup;
    private final JList<String> list; // einfache Liste mit Namen und *Namen

    private String chosenName; // z. B. "username" oder "*otpCode"

    public ScopeReferenceComboBox() {
        super(new BorderLayout());

        // Nicht-editierbares "Anzeige"-Feld
        displayField = new JTextField();
        displayField.setEditable(false);

        // Kleiner Button mit "▼"
        dropButton = new JButton("▼");
        dropButton.setMargin(new Insets(0, 4, 0, 4));
        dropButton.setFocusable(false);

        // Liste + Scrollpane für das Popup
        list = new JList<String>(new DefaultListModel<String>());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(200, 160));

        popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        popup.add(scroll, BorderLayout.CENTER);

        add(displayField, BorderLayout.CENTER);
        add(dropButton, BorderLayout.EAST);

        wireInteractions();
    }

    /**
     * Übergib die aktuell sichtbaren Scope-Namen.
     * Reihenfolge laut Anforderung:
     *   1. "normale" Variablen (BeforeEach usw.) ohne Stern
     *   2. Templates mit führendem "*"
     *
     * Wir legen sie in ein DefaultListModel<String>, das wir in der JList anzeigen.
     *
     * Hinweis:
     *  - Die Datenquelle "ScopeData" ist erstmal ein einfaches DTO mit zwei Sets.
     *    Du kannst sie später durch den echten GivenLookupService füllen.
     */
    public void setScopeData(ScopeData data) {
        DefaultListModel<String> model = new DefaultListModel<String>();

        if (data != null) {
            // 1) Variablen ohne Stern, sortiert für deterministische Anzeige
            java.util.List<String> varsSorted = new ArrayList<String>(data.variables);
            java.util.Collections.sort(varsSorted, String.CASE_INSENSITIVE_ORDER);
            for (String v : varsSorted) {
                if (v != null && v.trim().length() > 0) {
                    model.addElement(v.trim());
                }
            }

            // 2) Templates mit führendem '*', ebenfalls sortiert
            java.util.List<String> tmplSorted = new ArrayList<String>(data.templates);
            java.util.Collections.sort(tmplSorted, String.CASE_INSENSITIVE_ORDER);
            for (String t : tmplSorted) {
                if (t != null && t.trim().length() > 0) {
                    model.addElement("*" + t.trim());
                }
            }
        }

        list.setModel(model);
    }

    /**
     * Liefert den aktuell gewählten Namen zurück.
     * Kann z. B. "username" oder "*otpCode" sein.
     *
     * Wenn der User noch nichts ausgewählt hat, kann das null sein.
     */
    public String getChosenName() {
        return chosenName;
    }

    /**
     * Optionaler Helfer: von außen eine Vorauswahl setzen (z. B. vorhandener Wert aus TestAction.value)
     */
    public void setChosenName(String name) {
        this.chosenName = name;
        displayField.setText(name != null ? name : "");
    }

    // --------------------------------------------------------------------
    // interne Interaktion (Popup öffnen, Item bestätigen, etc.)
    // --------------------------------------------------------------------
    private void wireInteractions() {
        // Popup öffnen/schließen per Button
        dropButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPopup();
            }
        });

        // ENTER in der Liste = Auswahl übernehmen
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (KeyEvent.VK_ENTER == e.getKeyCode()) {
                    acceptSelection();
                } else if (KeyEvent.VK_ESCAPE == e.getKeyCode()) {
                    hidePopup();
                }
            }
        });

        // Doppelklick in der Liste = Auswahl übernehmen
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    acceptSelection();
                }
            }
        });

        // Falls Fokus verloren geht, Popup einfach schließen.
        // (Ist optional. Ohne das bleibt das Popup offen, ist aber okay.)
        list.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // wir schließen nur, wenn das Popup noch offen ist
                // und der Fokus wirklich ganz woanders hingeht.
                // Das ist eher UX-Kosmetik, nicht zwingend.
                if (popup.isVisible()) {
                    // popup.setVisible(false);  // wenn du willst, auskommentieren
                }
            }
        });
    }

    private void showPopup() {
        // Popup direkt unter diese Komponente platzieren
        popup.show(this, 0, this.getHeight());

        // ersten Eintrag vorauswählen für schnell Enter
        if (list.getModel().getSize() > 0 && list.getSelectedIndex() < 0) {
            list.setSelectedIndex(0);
        }

        list.requestFocusInWindow();
    }

    private void hidePopup() {
        popup.setVisible(false);
    }

    /**
     * Übernimmt das aktuell in der Liste selektierte Element als chosenName.
     * Aktualisiert displayField und schließt das Popup.
     */
    private void acceptSelection() {
        String sel = list.getSelectedValue();
        if (sel != null) {
            chosenName = sel;
            displayField.setText(sel);
        }
        hidePopup();
    }

    // --------------------------------------------------------------------
    // DTO-Klasse ScopeData
    // --------------------------------------------------------------------
    /**
     * Mini-DTO für setScopeData().
     *
     * variables = alle sichtbaren "normalen" Namen, z. B. aus BeforeEach eines Cases,
     *             aus BeforeEach einer Suite, aus BeforeEach des Roots.
     *
     * templates = alle sichtbaren Template-Namen (lazy Funktionen),
     *             z. B. Templates-Liste Case, Suite, Root.
     *
     * BeforeAll-Werte gehören NICHT in dieses Dropdown (laut Anforderung),
     * deswegen tauchen sie hier gar nicht erst auf.
     */
    public static class ScopeData {
        public final Set<String> variables = new LinkedHashSet<String>();
        public final Set<String> templates = new LinkedHashSet<String>();

        public ScopeData() {
        }

        public ScopeData(Collection<String> vars, Collection<String> tmpls) {
            if (vars != null) {
                variables.addAll(vars);
            }
            if (tmpls != null) {
                templates.addAll(tmpls);
            }
        }
    }
}
