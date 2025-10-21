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

/** Edit a TestCase (Given/When/Then). Einheitlicher Given-Editor (GivenConditionEditorTab) überall. */
public class CaseEditorTab extends AbstractEditorTab<TestCase> {

    private static final String TYPE_PRECONDITION_REF = "preconditionRef";

    private final DefaultListModel<Object> stepListModel = new DefaultListModel<>();
    private final JList<Object> stepList = new JList<>(stepListModel);
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
            @Override public void actionPerformed(ActionEvent e) {
                addGivenViaDialog();
            }
        }));
        toolbar.add(createToolbarButton("+ When", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                addStep(new TestAction("click"));
            }
        }));
        toolbar.add(createToolbarButton("+ Then", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                addStep(new ThenExpectation());
            }
        }));
        toolbar.addSeparator();
        toolbar.add(createToolbarButton("↑", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                moveStep(-1);
            }
        }));
        toolbar.add(createToolbarButton("↓", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                moveStep(1);
            }
        }));
        toolbar.add(createToolbarButton("🗑", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
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

    private void reloadList() {
        stepListModel.clear();
        TestCase testCase = getModel();
        for (GivenCondition g : testCase.getGiven()) stepListModel.addElement(g);
        for (TestAction w : testCase.getWhen()) stepListModel.addElement(w);
        for (ThenExpectation t : testCase.getThen()) stepListModel.addElement(t);
    }

    private void addStep(Object step) {
        if (step instanceof GivenCondition) {
            getModel().getGiven().add((GivenCondition) step);
        } else if (step instanceof TestAction) {
            getModel().getWhen().add((TestAction) step);
        } else if (step instanceof ThenExpectation) {
            getModel().getThen().add((ThenExpectation) step);
        }
        reloadList();
        TestRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(suite != null ? suite.getName() : null));
    }

    private void deleteStep() {
        int index = stepList.getSelectedIndex();
        if (index >= 0) {
            Object step = stepListModel.get(index);
            if (step instanceof GivenCondition) getModel().getGiven().remove(step);
            else if (step instanceof TestAction) getModel().getWhen().remove(step);
            else if (step instanceof ThenExpectation) getModel().getThen().remove(step);
            reloadList();
            detailPanel.removeAll();
            detailPanel.revalidate();
            detailPanel.repaint();
            ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(suite != null ? suite.getName() : null));
        }
    }

    private void moveStep(int direction) {
        int index = stepList.getSelectedIndex();
        if (index < 0 || (direction == -1 && index == 0) || (direction == 1 && index == stepListModel.size() - 1)) return;

        Object current = stepListModel.get(index);
        stepListModel.remove(index);
        stepListModel.add(index + direction, current);
        stepList.setSelectedIndex(index + direction);

        if (current instanceof GivenCondition) {
            List<GivenCondition> list = getModel().getGiven();
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

        ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(suite != null ? suite.getName() : null));
    }

    private void updateDetailPanel(Object selected) {
        detailPanel.removeAll();
        if (selected instanceof TestAction) {
            detailPanel.add(new ActionEditorTab((TestAction) selected), BorderLayout.CENTER);
        } else if (selected instanceof GivenCondition) {
            detailPanel.add(new GivenConditionEditorTab((GivenCondition) selected), BorderLayout.CENTER);
        } else if (selected instanceof ThenExpectation) {
            detailPanel.add(new ThenExpectationEditorTab((ThenExpectation) selected), BorderLayout.CENTER);
        }
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    private JButton createToolbarButton(String text, AbstractAction action) {
        JButton button = new JButton(action);
        button.setText(text);
        return button;
    }

    /** Unified picker (Given types + Preconditions). */
    private void addGivenViaDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        GivenChoiceDialog dlg = new GivenChoiceDialog(owner, "Given hinzufügen", null);
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) return;

        int kind = dlg.getSelectedKind();
        String idOrType = dlg.getIdOrType();

        if (kind == GivenChoiceDialog.KIND_GIVEN_TYPE) {
            GivenCondition gc = new GivenCondition();
            gc.setType(idOrType);
            addStep(gc);
            stepList.setSelectedValue(gc, true);
        } else if (kind == GivenChoiceDialog.KIND_PRECONDITION) {
            GivenCondition gc = new GivenCondition();
            gc.setType(TYPE_PRECONDITION_REF);
            gc.setValue("id=" + idOrType);
            addStep(gc);
            stepList.setSelectedValue(gc, true);
        }
    }

    private String parseIdFromValue(String value) {
        if (value == null) return "";
        String[] pairs = value.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && "id".equals(kv[0])) return kv[1];
        }
        return "";
    }

    private String resolvePreconditionName(String id) {
        if (id == null || id.trim().isEmpty()) return "(keine)";
        List<Precondition> list = PreconditionRegistry.getInstance().getAll();
        for (Precondition p : list) {
            if (id.equals(p.getId())) {
                String n = p.getName();
                return (n != null && n.trim().length() > 0) ? n.trim() : "(unnamed)";
            }
        }
        return id;
    }

    /** Renderer zeigt bei preconditionRef den Namen + UUID an. */
    private class StepListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
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
