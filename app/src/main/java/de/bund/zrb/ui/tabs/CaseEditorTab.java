package de.bund.zrb.ui.tabs;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.Precondition;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.model.ThenExpectation;
import de.bund.zrb.service.PreconditionRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.dialogs.GivenChoiceDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Edit a TestCase (Given/When/Then).
 *
 * Intent:
 * - Show all steps (Given, When, Then) in one list.
 * - When a step is selected, render a suitable editor on the right side.
 *
 * Key points:
 * - GivenConditionEditorTab: bearbeite ein einzelnes Given (User, Vorlage/Precondition, Ausdruck mit AST-Vorschau)
 * - ActionEditorTab: bearbeite eine einzelne When-Action
 *      -> bekommt jetzt zusätzlich alle Givens des aktuellen TestCase,
 *         damit die Value-Dropdown-TreeView echte Knoten aus den Givens anzeigen kann.
 * - ThenExpectationEditorTab: bearbeite eine einzelne Then-Expectation
 *
 * Persistenz:
 * - Nach Änderungen wird TestRegistry.getInstance().save() gerufen,
 *   und es wird ein TestSuiteSavedEvent gepublished.
 */
public class CaseEditorTab extends AbstractEditorTab<TestCase> {

    private static final String TYPE_PRECONDITION_REF = "preconditionRef";

    private final DefaultListModel<Object> stepListModel = new DefaultListModel<Object>();
    private final JList<Object> stepList = new JList<Object>(stepListModel);
    private final JPanel detailPanel = new JPanel(new BorderLayout());
    private final TestSuite suite;

    public CaseEditorTab(TestSuite suiteRef, TestCase testCase) {
        super("Test Case: " + testCase.getName(), testCase);
        this.suite = suiteRef;

        setLayout(new BorderLayout());

        stepList.setCellRenderer(new StepListRenderer());
        stepList.addListSelectionListener(e -> updateDetailPanel(stepList.getSelectedValue()));

        JScrollPane scrollPane = new JScrollPane(stepList);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        toolbar.add(createToolbarButton("+ Given", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addGivenViaDialog();
            }
        }));
        toolbar.add(createToolbarButton("+ When", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addStep(new TestAction("click"));
            }
        }));
        toolbar.add(createToolbarButton("+ Then", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addStep(new ThenExpectation());
            }
        }));
        toolbar.addSeparator();
        toolbar.add(createToolbarButton("↑", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveStep(-1);
            }
        }));
        toolbar.add(createToolbarButton("↓", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveStep(1);
            }
        }));
        toolbar.add(createToolbarButton("🗑", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteStep();
            }
        }));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, detailPanel);
        split.setResizeWeight(0.3);
        split.setOneTouchExpandable(true);

        add(toolbar, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        reloadList();
    }

    /**
     * Reload the left list (Given, When, Then) from the current TestCase model.
     * Keep ordering: all Given first, then When, then Then.
     */
    private void reloadList() {
        stepListModel.clear();
        TestCase testCase = getModel();
        for (GivenCondition g : testCase.getBefore()) {
            stepListModel.addElement(g);
        }
        for (TestAction w : testCase.getWhen()) {
            stepListModel.addElement(w);
        }
        for (ThenExpectation t : testCase.getThen()) {
            stepListModel.addElement(t);
        }
    }

    /**
     * Add a step to the correct section of the TestCase
     * (Given / When / Then), then persist and refresh UI.
     */
    private void addStep(Object step) {
        if (step instanceof GivenCondition) {
            getModel().getBefore().add((GivenCondition) step);
        } else if (step instanceof TestAction) {
            getModel().getWhen().add((TestAction) step);
        } else if (step instanceof ThenExpectation) {
            getModel().getThen().add((ThenExpectation) step);
        }

        reloadList();
        TestRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(
                new TestSuiteSavedEvent(suite != null ? suite.getName() : null)
        );
    }

    /**
     * Delete the currently selected step from the model and refresh everything.
     */
    private void deleteStep() {
        int index = stepList.getSelectedIndex();
        if (index >= 0) {
            Object step = stepListModel.get(index);

            if (step instanceof GivenCondition) {
                getModel().getBefore().remove(step);
            } else if (step instanceof TestAction) {
                getModel().getWhen().remove(step);
            } else if (step instanceof ThenExpectation) {
                getModel().getThen().remove(step);
            }

            reloadList();
            detailPanel.removeAll();
            detailPanel.revalidate();
            detailPanel.repaint();

            ApplicationEventBus.getInstance().publish(
                    new TestSuiteSavedEvent(suite != null ? suite.getName() : null)
            );
        }
    }

    /**
     * Move the selected step up (-1) or down (+1) in the overall list
     * and also in the underlying model section.
     */
    private void moveStep(int direction) {
        int index = stepList.getSelectedIndex();
        if (index < 0 ||
                (direction == -1 && index == 0) ||
                (direction == 1 && index == stepListModel.size() - 1)) {
            return;
        }

        Object current = stepListModel.get(index);
        stepListModel.remove(index);
        stepListModel.add(index + direction, current);
        stepList.setSelectedIndex(index + direction);

        if (current instanceof GivenCondition) {
            List<GivenCondition> list = getModel().getBefore();
            list.remove(current);
            list.add(index + direction, (GivenCondition) current);
        } else if (current instanceof TestAction) {
            List<TestAction> list = getModel().getWhen();
            list.remove(current);
            list.add(index + direction, (TestAction) current);
        } else if (current instanceof ThenExpectation) {
            List<ThenExpectation> list = getModel().getThen();
            list.remove(current);
            list.add(index + direction, (ThenExpectation) current);
        }

        ApplicationEventBus.getInstance().publish(
                new TestSuiteSavedEvent(suite != null ? suite.getName() : null)
        );
    }

    /**
     * Update the right-hand detail panel based on which step is selected.
     *
     * - When selecting a TestAction:
     *   Create ActionEditorTab with BOTH the TestAction AND the full Given list
     *   from this TestCase, so the Value dropdown in ActionEditorTab can show
     *   all expressions of all Givens.
     *
     * - When selecting a GivenCondition:
     *   Show GivenConditionEditorTab for that Given.
     *
     * - When selecting a ThenExpectation:
     *   Show ThenExpectationEditorTab.
     */
    private void updateDetailPanel(Object selected) {
        detailPanel.removeAll();

        if (selected instanceof TestAction) {
            TestAction ta = (TestAction) selected;

            // >>> WICHTIGER TEIL: hier geben wir die Givens dieses TestCase mit
            List<GivenCondition> givensForThisCase = getModel().getBefore();

            detailPanel.add(
                    new ActionEditorTab(ta, givensForThisCase),
                    BorderLayout.CENTER
            );

        } else if (selected instanceof GivenCondition) {
            detailPanel.add(
                    new GivenConditionEditorTab((GivenCondition) selected),
                    BorderLayout.CENTER
            );

        } else if (selected instanceof ThenExpectation) {
            detailPanel.add(
                    new ThenExpectationEditorTab((ThenExpectation) selected),
                    BorderLayout.CENTER
            );
        }

        detailPanel.revalidate();
        detailPanel.repaint();
    }

    /**
     * Create a small JButton for the toolbar with the provided label and action.
     */
    private JButton createToolbarButton(String text, AbstractAction action) {
        JButton button = new JButton(action);
        button.setText(text);
        return button;
    }

    /**
     * Dialog zum Hinzufügen eines neuen Given-Schritts.
     *
     * Aktuell:
     * - Wir unterstützen nur noch Precondition-Referenzen als Givens.
     * - Der Dialog liefert uns die ausgewählte Precondition-UUID (oder "").
     * - Wir legen ein neues GivenCondition an vom Typ "preconditionRef"
     *   und speichern die UUID in value als "id=<uuid>".
     *
     * Danach wählen wir das neue Given sofort in der Liste aus.
     */
    private void addGivenViaDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        GivenChoiceDialog dlg = new GivenChoiceDialog(owner, "Given hinzufügen", null);
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) return;

        // In der neuen Welt liefert der Dialog immer eine Precondition-Auswahl.
        String idOrType = dlg.getIdOrType(); // UUID oder "" (leer)

        GivenCondition gc = new GivenCondition();
        gc.setType(TYPE_PRECONDITION_REF); // "preconditionRef"
        gc.setValue("id=" + idOrType);

        addStep(gc);

        // Wähle das neue Given direkt in der Liste aus
        stepList.setSelectedValue(gc, true);
    }

    /**
     * Extract "id" from the GivenCondition.value string.
     * value wird als key=value&key2=value2 gespeichert.
     */
    private String parseIdFromValue(String value) {
        if (value == null) return "";
        String[] pairs = value.split("&");
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && "id".equals(kv[0])) {
                return kv[1];
            }
        }
        return "";
    }

    /**
     * Resolve Precondition name by UUID for display in the step list.
     * Falls kein Name gepflegt ist, zeige "(unnamed)".
     * Falls keine UUID, zeige "(keine)".
     */
    private String resolvePreconditionName(String id) {
        if (id == null || id.trim().isEmpty()) return "(keine)";
        List<Precondition> list = PreconditionRegistry.getInstance().getAll();
        for (int i = 0; i < list.size(); i++) {
            Precondition p = list.get(i);
            if (id.equals(p.getId())) {
                String n = p.getName();
                return (n != null && n.trim().length() > 0) ? n.trim() : "(unnamed)";
            }
        }
        return id;
    }

    /**
     * Renderer for the left list. Shows:
     * - For GivenCondition of type "preconditionRef": "Given: Precondition → <name> {uuid}"
     * - For other Givens (legacy): "Given: <type>"
     * - For TestAction: "When: <action>"
     * - For ThenExpectation: "Then: <type>"
     *
     * Intent:
     * - Make it obvious what each row represents to the tester.
     */
    private class StepListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus
            );

            if (value instanceof GivenCondition) {
                GivenCondition gc = (GivenCondition) value;
                if (TYPE_PRECONDITION_REF.equals(gc.getType())) {
                    String id = parseIdFromValue(gc.getValue());
                    String name = resolvePreconditionName(id);
                    label.setText("Given: Precondition → " + name + " {" + id + "}");
                } else {
                    label.setText("Given: " + gc.getType());
                }
            } else if (value instanceof TestAction) {
                label.setText("When: " + ((TestAction) value).getAction());
            } else if (value instanceof ThenExpectation) {
                label.setText("Then: " + ((ThenExpectation) value).getType());
            }

            return label;
        }
    }
}
