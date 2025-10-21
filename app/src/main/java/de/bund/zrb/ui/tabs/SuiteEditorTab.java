package de.bund.zrb.ui.tabs;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.Precondition;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.PreconditionRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.dialogs.GivenChoiceDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/** Edit a TestSuite (Given + Cases). Add unified Given picker and support preconditionRef. */
public class SuiteEditorTab extends AbstractEditorTab<TestSuite> {

    private static final String TYPE_PRECONDITION_REF = "preconditionRef";

    private final DefaultListModel<Object> listModel = new DefaultListModel<Object>();
    private final JList<Object> list = new JList<Object>(listModel);
    private final JPanel detailPanel = new JPanel(new BorderLayout());

    public SuiteEditorTab(TestSuite suite) {
        super("Test Suite: " + suite.getName(), suite);
        setLayout(new BorderLayout());

        list.setCellRenderer(new SuiteRenderer());
        list.addListSelectionListener(e -> updateDetailPanel(list.getSelectedValue()));

        JScrollPane scroll = new JScrollPane(list);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        toolbar.add(createToolbarButton("+ Given", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                addGivenViaDialog();
            }
        }));
        toolbar.addSeparator();
        toolbar.add(createToolbarButton("+ Case", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                addCase();
            }
        }));
        toolbar.add(createToolbarButton("↑", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                moveItem(-1);
            }
        }));
        toolbar.add(createToolbarButton("↓", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                moveItem(1);
            }
        }));
        toolbar.add(createToolbarButton("🗑", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                deleteItem();
            }
        }));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, detailPanel);
        split.setResizeWeight(0.35);
        split.setOneTouchExpandable(true);

        add(toolbar, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        reload();
    }

    private void reload() {
        listModel.clear();
        TestSuite s = getModel();
        for (GivenCondition g : s.getGiven()) listModel.addElement(g);
        for (TestCase c : s.getTestCases()) listModel.addElement(c);
    }

    private void addGivenViaDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        GivenChoiceDialog dlg = new GivenChoiceDialog(owner, "Given hinzufügen", null);
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) return;

        int kind = dlg.getSelectedKind();
        String idOrType = dlg.getIdOrType();

        GivenCondition gc = new GivenCondition();
        if (kind == GivenChoiceDialog.KIND_GIVEN_TYPE) {
            gc.setType(idOrType);
        } else if (kind == GivenChoiceDialog.KIND_PRECONDITION) {
            gc.setType(TYPE_PRECONDITION_REF);
            gc.setValue("id=" + idOrType);
        } else {
            return;
        }
        getModel().getGiven().add(gc);
        TestRegistry.getInstance().save();

        reload();
        list.setSelectedValue(gc, true);
        ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(getModel().getName()));
    }

    private void addCase() {
        String name = JOptionPane.showInputDialog(this, "Name des neuen TestCase:", "Neuer TestCase", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().length() == 0) return;
        TestCase tc = new TestCase(name.trim(), new java.util.ArrayList<TestAction>());
        getModel().getTestCases().add(tc);
        TestRegistry.getInstance().save();
        reload();
        list.setSelectedValue(tc, true);
        ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(getModel().getName()));
    }

    private void deleteItem() {
        int idx = list.getSelectedIndex();
        if (idx < 0) return;
        Object sel = listModel.get(idx);
        if (sel instanceof GivenCondition) {
            getModel().getGiven().remove(sel);
        } else if (sel instanceof TestCase) {
            getModel().getTestCases().remove(sel);
        }
        TestRegistry.getInstance().save();
        reload();
        detailPanel.removeAll();
        detailPanel.revalidate();
        detailPanel.repaint();
        ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(getModel().getName()));
    }

    private void moveItem(int dir) {
        int idx = list.getSelectedIndex();
        if (idx < 0) return;
        if (dir < 0 && idx == 0) return;
        if (dir > 0 && idx == listModel.size() - 1) return;

        Object current = listModel.get(idx);

        // Compute boundary between Given and Cases
        int givenCount = getModel().getGiven().size();

        if (current instanceof GivenCondition) {
            java.util.List<GivenCondition> gl = getModel().getGiven();
            int inx = gl.indexOf(current);
            if (inx < 0) return;
            int target = inx + dir;
            if (target < 0 || target >= gl.size()) return;
            gl.remove(inx);
            gl.add(target, (GivenCondition) current);
        } else if (current instanceof TestCase) {
            java.util.List<TestCase> cl = getModel().getTestCases();
            int inx = cl.indexOf(current);
            if (inx < 0) return;
            int target = inx + dir;
            if (target < 0 || target >= cl.size()) return;
            cl.remove(inx);
            cl.add(target, (TestCase) current);
        }
        TestRegistry.getInstance().save();
        reload();
        list.setSelectedValue(current, true);
        ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(getModel().getName()));
    }

    private void updateDetailPanel(Object selected) {
        detailPanel.removeAll();
        if (selected instanceof GivenCondition) {
            GivenCondition gc = (GivenCondition) selected;
            if (TYPE_PRECONDITION_REF.equals(gc.getType())) {
                detailPanel.add(buildPreconditionRefEditor(gc), BorderLayout.CENTER);
            } else {
                detailPanel.add(new GivenConditionEditorTab(gc), BorderLayout.CENTER);
            }
        } else if (selected instanceof TestCase) {
            detailPanel.add(new CaseEditorTab(getModel(), (TestCase) selected), BorderLayout.CENTER);
        }
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    private JButton createToolbarButton(String text, AbstractAction action) {
        JButton b = new JButton(action);
        b.setText(text);
        return b;
    }

    /** Build a tiny editor to show and re-pick a referenced precondition (by UUID). */
    private JComponent buildPreconditionRefEditor(final GivenCondition gc) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        final String id = parseIdFromValue(gc.getValue());
        final String name = resolvePreconditionName(id);

        JPanel top = new JPanel(new GridLayout(0, 1, 4, 4));
        top.add(new JLabel("Precondition: " + name));
        top.add(new JLabel("ID: " + id));
        p.add(top, BorderLayout.NORTH);

        JButton change = new JButton("Precondition auswählen…");
        change.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                java.awt.Window owner = SwingUtilities.getWindowAncestor(SuiteEditorTab.this);
                GivenChoiceDialog dlg = new GivenChoiceDialog(owner, "Precondition wählen", id);
                dlg.setVisible(true);
                if (!dlg.isConfirmed()) return;
                if (dlg.getSelectedKind() != GivenChoiceDialog.KIND_PRECONDITION) return;

                String newId = dlg.getIdOrType();
                if (newId != null && newId.length() > 0 && !newId.equals(id)) {
                    gc.setValue("id=" + newId);
                    TestRegistry.getInstance().save();
                    updateDetailPanel(gc);
                    list.repaint();
                }
            }
        });

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(change);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private String parseIdFromValue(String value) {
        if (value == null) return "";
        String[] pairs = value.split("&");
        for (int i = 0; i < pairs.length; i++) {
            String[] kv = pairs[i].split("=", 2);
            if (kv.length == 2 && "id".equals(kv[0])) return kv[1];
        }
        return "";
    }

    private String resolvePreconditionName(String id) {
        if (id == null || id.trim().length() == 0) return "(keine)";
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

    private class SuiteRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> listComp, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(listComp, value, index, isSelected, cellHasFocus);
            if (value instanceof GivenCondition) {
                GivenCondition gc = (GivenCondition) value;
                if (TYPE_PRECONDITION_REF.equals(gc.getType())) {
                    String id = parseIdFromValue(gc.getValue());
                    String name = resolvePreconditionName(id);
                    label.setText("Given: Precondition → " + name + " {" + id + "}");
                } else {
                    label.setText("Given: " + gc.getType());
                }
            } else if (value instanceof TestCase) {
                label.setText("Case: " + ((TestCase) value).getName());
            }
            return label;
        }
    }
}
