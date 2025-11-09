package de.bund.zrb.ui.leftdrawer;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.PreconditionSavedEvent;
import de.bund.zrb.model.Precondition;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.service.PreconditionRegistry;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.dialogs.ActionPickerDialog;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulate all behavior for the "Preconditions" tree to keep LeftDrawer slim.
 * Methods keep the look & behavior consistent with the Tests tree. THEN is irrelevant here.
 */
public class PrecondTreeController {

    private final JTree precondTree;

    // Optionaler Handler, um einen Node in einem neuen (persistenten) Tab zu öffnen
    private NodeOpenHandler openHandler;

    public PrecondTreeController(JTree precondTree) {
        this.precondTree = precondTree;
    }

    public void setOpenHandler(NodeOpenHandler openHandler) {
        this.openHandler = openHandler;
    }

    // ========================= Preconditions tab =========================

    public static JTree buildPrecondTree() {
        TestNode root = new TestNode("Preconditions");
        return new JTree(root);
    }

    public void refreshPreconditions() {
        TestNode root = new TestNode("Preconditions");

        for (Precondition p : PreconditionRegistry.getInstance().getAll()) {
            // Show name; optionally include (id) to help debugging
            String display = (p.getName() != null && !p.getName().trim().isEmpty())
                    ? p.getName().trim()
                    : "(unnamed)";
            TestNode preNode = new TestNode(display, p);

            // List actions (When) as children
            if (p.getActions() != null) {
                for (TestAction action : p.getActions()) {
                    String label = renderActionLabel(action);
                    TestNode stepNode = new TestNode(label, action);
                    preNode.add(stepNode);
                }
            }

            root.add(preNode);
        }

        DefaultTreeModel model = (DefaultTreeModel) precondTree.getModel();
        model.setRoot(root);
        model.reload();
        // Expand root for better visibility
        precondTree.expandPath(new TreePath(root.getPath()));
    }

    public String renderActionLabel(TestAction action) {
        String label = action.getAction();
        if (action.getValue() != null && !action.getValue().isEmpty()) {
            label += " [" + action.getValue() + "]";
        } else if (action.getSelectedSelector() != null && !action.getSelectedSelector().isEmpty()) {
            label += " [" + action.getSelectedSelector() + "]";
        }
        return label;
    }

    // ========================= Context menu (preconditions) =========================

    /**
     * Set up a dynamic context menu for the precondition tree. Depending on the clicked node:
     * - Blank area: new precondition.
     * - Precondition: new precondition after + copy precondition + add step; then common items.
     * - Step: new step after + copy step; then common items.
     */
    public void setupContextMenu() {
        precondTree.setComponentPopupMenu(null);
        precondTree.addMouseListener(new java.awt.event.MouseAdapter() {
            private void handlePopup(java.awt.event.MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int x = e.getX();
                int y = e.getY();
                TreePath path = precondTree.getPathForLocation(x, y);
                TestNode clicked = null;
                if (path != null) {
                    Object n = path.getLastPathComponent();
                    if (n instanceof TestNode) {
                        clicked = (TestNode) n;
                        precondTree.setSelectionPath(path);
                    }
                }
                JPopupMenu menu = buildContextMenu(clicked);
                menu.show(e.getComponent(), x, y);
            }
            @Override public void mousePressed(java.awt.event.MouseEvent e) { handlePopup(e); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { handlePopup(e); }
        });
    }

    public JPopupMenu buildContextMenu(TestNode clicked) {
        JPopupMenu menu = new JPopupMenu();

        if (clicked == null) {
            JMenuItem newPre = new JMenuItem("Neue Precondition");
            newPre.addActionListener(evt -> createNewPreconditionAfter(null));
            menu.add(newPre);
            return menu;
        }

        Object ref = clicked.getModelRef();

        // Einheitliche Öffnen-Aktion (persistenter Tab) – hier sinnvoll für Steps
        if (openHandler != null && (ref instanceof TestAction || ref instanceof Precondition)) {
            JMenuItem openPersistent = new JMenuItem("In neuem Tab öffnen");
            openPersistent.addActionListener(evt -> openHandler.openInNewTab(clicked));
            menu.add(openPersistent);
            menu.addSeparator();
        }

        if (ref instanceof Precondition) {
            JMenuItem newPre = new JMenuItem("Neue Precondition");
            newPre.addActionListener(evt -> createNewPreconditionAfter(clicked));
            menu.add(newPre);

            JMenuItem copyPre = new JMenuItem("Kopie von Precondition");
            copyPre.addActionListener(evt -> duplicatePreconditionAfter(clicked));
            menu.add(copyPre);

            JMenuItem addStep = new JMenuItem("Neuen Schritt hinzufügen");
            addStep.addActionListener(evt -> createNewStepUnderPrecondition(clicked));
            menu.add(addStep);

            menu.addSeparator();
            addCommonMenuItems(menu, clicked);
            return menu;
        }

        if (ref instanceof TestAction) {
            JMenuItem newStep = new JMenuItem("Neuer Schritt");
            newStep.addActionListener(evt -> createNewStepAfter(clicked));
            menu.add(newStep);

            JMenuItem dupStep = new JMenuItem("Kopie von Schritt");
            dupStep.addActionListener(evt -> duplicateStepAfter(clicked));
            menu.add(dupStep);

            menu.addSeparator();
            addCommonMenuItems(menu, clicked);
            return menu;
        }

        // Fallback
        addCommonMenuItems(menu, clicked);
        return menu;
    }

    private void addCommonMenuItems(JPopupMenu menu, TestNode onNode) {
        JMenuItem renameItem = new JMenuItem("Umbenennen");
        renameItem.addActionListener(evt -> renameNode());
        menu.add(renameItem);

        JMenuItem deleteItem = new JMenuItem("Löschen");
        deleteItem.addActionListener(evt -> deleteNode());
        menu.add(deleteItem);
    }

    // ========================= Create / duplicate / rename / delete =========================

    /** Create a new precondition directly after clicked (or append at root when null). */
    public void createNewPreconditionAfter(TestNode clickedPre) {
        String name = JOptionPane.showInputDialog(precondTree, "Name der neuen Precondition:", "Neue Precondition", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        Precondition newPre = new Precondition();
        newPre.setId(java.util.UUID.randomUUID().toString());
        newPre.setName(name.trim());

        List<Precondition> list = PreconditionRegistry.getInstance().getAll();
        int insertIndex = (clickedPre != null && clickedPre.getModelRef() instanceof Precondition)
                ? ((DefaultMutableTreeNode) clickedPre.getParent()).getIndex(clickedPre) + 1
                : list.size();

        list.add(insertIndex, newPre);
        PreconditionRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(new PreconditionSavedEvent(newPre.getName()));

        DefaultTreeModel model = (DefaultTreeModel) precondTree.getModel();
        TestNode root = (TestNode) model.getRoot();
        TestNode parent = root;
        TestNode newNode = new TestNode(newPre.getName(), newPre);

        if (clickedPre != null && clickedPre.getParent() != null) {
            parent = (TestNode) clickedPre.getParent();
            model.insertNodeInto(newNode, parent, Math.min(insertIndex, parent.getChildCount()));
        } else {
            model.insertNodeInto(newNode, parent, Math.min(insertIndex, parent.getChildCount()));
        }

        selectNode(newNode);
    }

    /** Duplicate a precondition and insert directly after the clicked one. */
    public void duplicatePreconditionAfter(TestNode clickedPre) {
        if (clickedPre == null || !(clickedPre.getModelRef() instanceof Precondition)) return;

        Precondition src = (Precondition) clickedPre.getModelRef();

        Precondition copy = new Precondition();
        copy.setId(java.util.UUID.randomUUID().toString());
        copy.setName(uniquePreconditionName("copy of " + safeName(src.getName())));

        // Shallow-copy given, deep-copy actions
        if (src.getGiven() != null) {
            copy.getGiven().addAll(src.getGiven());
        }
        if (src.getActions() != null) {
            for (TestAction a : src.getActions()) {
                copy.getActions().add(cloneActionShallow(a, false));
            }
        }

        List<Precondition> list = PreconditionRegistry.getInstance().getAll();
        int insertIndex = ((DefaultMutableTreeNode) clickedPre.getParent()).getIndex(clickedPre) + 1;
        list.add(insertIndex, copy);
        PreconditionRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(new PreconditionSavedEvent(copy.getName()));

        DefaultTreeModel model = (DefaultTreeModel) precondTree.getModel();
        TestNode root = (TestNode) model.getRoot();
        TestNode newNode = new TestNode(copy.getName(), copy);
        model.insertNodeInto(newNode, root, Math.min(insertIndex, root.getChildCount()));
        selectNode(newNode);
    }

    /** Create a new step under a precondition (at the end). */
    public void createNewStepUnderPrecondition(TestNode preNode) {
        if (preNode == null || !(preNode.getModelRef() instanceof Precondition)) return;

        // Use action picker instead of free text
        Window owner = SwingUtilities.getWindowAncestor(precondTree);
        ActionPickerDialog dlg = new ActionPickerDialog(owner, "Neuer Schritt", "click");
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) return;

        String actionName = dlg.getChosenAction();
        if (actionName.length() == 0) return;

        Precondition pre = (Precondition) preNode.getModelRef();
        TestAction newAction = new TestAction(actionName);
        pre.getActions().add(newAction);

        DefaultTreeModel model = (DefaultTreeModel) precondTree.getModel();
        TestNode newStepNode = new TestNode(renderActionLabel(newAction), newAction);
        model.insertNodeInto(newStepNode, preNode, preNode.getChildCount());
        precondTree.expandPath(new TreePath(preNode.getPath()));

        PreconditionRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(new PreconditionSavedEvent(pre.getName()));
    }


    /** Create a new step directly after the given step. */
    public void createNewStepAfter(TestNode clickedStep) {
        if (clickedStep == null || !(clickedStep.getModelRef() instanceof TestAction)) return;

        Window owner = SwingUtilities.getWindowAncestor(precondTree);
        ActionPickerDialog dlg = new ActionPickerDialog(owner, "Neuer Schritt", "click");
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) return;

        String actionName = dlg.getChosenAction();
        if (actionName.length() == 0) return;

        TestAction oldAction = (TestAction) clickedStep.getModelRef();
        TestNode preNode = (TestNode) clickedStep.getParent();
        if (preNode == null || !(preNode.getModelRef() instanceof Precondition)) return;

        Precondition pre = (Precondition) preNode.getModelRef();
        TestAction newAction = new TestAction(actionName);
        List<TestAction> actions = pre.getActions();

        int idx = actions.indexOf(oldAction);
        if (idx < 0) idx = actions.size() - 1;
        actions.add(idx + 1, newAction);

        DefaultTreeModel model = (DefaultTreeModel) precondTree.getModel();
        TestNode newStepNode = new TestNode(renderActionLabel(newAction), newAction);
        model.insertNodeInto(newStepNode, preNode, idx + 1);
        precondTree.expandPath(new TreePath(preNode.getPath()));

        PreconditionRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(new PreconditionSavedEvent(pre.getName()));
    }


    /** Duplicate a step (shallow copy) and insert directly after it. */
    public void duplicateStepAfter(TestNode clickedStep) {
        if (clickedStep == null || !(clickedStep.getModelRef() instanceof TestAction)) return;

        DefaultMutableTreeNode preNode = (DefaultMutableTreeNode) clickedStep.getParent();
        Object preRef = (preNode instanceof TestNode) ? ((TestNode) preNode).getModelRef() : null;
        if (!(preRef instanceof Precondition)) return;

        Precondition pre = (Precondition) preRef;
        TestAction src = (TestAction) clickedStep.getModelRef();

        TestAction copy = cloneActionShallow(src, true);

        List<TestAction> steps = pre.getActions();
        int insertIndex = preNode.getIndex(clickedStep) + 1;
        steps.add(insertIndex, copy);
        PreconditionRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(new PreconditionSavedEvent(pre.getName()));

        DefaultTreeModel model = (DefaultTreeModel) precondTree.getModel();
        TestNode newNode = new TestNode(renderActionLabel(copy), copy);
        model.insertNodeInto(newNode, preNode, Math.min(insertIndex, preNode.getChildCount()));
        selectNode(newNode);
    }

    /** Rename selected node: precondition (name) or step (action string). */
    public void renameNode() {
        TestNode selected = (TestNode) precondTree.getLastSelectedPathComponent();
        if (selected == null || selected.getParent() == null) return; // Root nicht umbenennen

        Object ref = selected.getModelRef();

        if (ref instanceof Precondition) {
            String current = ((Precondition) ref).getName();
            String name = JOptionPane.showInputDialog(precondTree, "Neuer Name:", current);
            if (name == null || name.trim().isEmpty()) return;

            ((Precondition) ref).setName(name.trim());
            selected.setUserObject(name.trim());
            ((DefaultTreeModel) precondTree.getModel()).nodeChanged(selected);

        } else if (ref instanceof TestAction) {
            TestAction a = (TestAction) ref;

            Window owner = SwingUtilities.getWindowAncestor(precondTree);
            ActionPickerDialog dlg = new ActionPickerDialog(owner, "Schritt umbenennen (Action)", a.getAction());
            dlg.setVisible(true);
            if (!dlg.isConfirmed()) return;

            String newAction = dlg.getChosenAction();
            if (newAction.length() == 0) return;

            a.setAction(newAction);
            selected.setUserObject(renderActionLabel(a));
            ((DefaultTreeModel) precondTree.getModel()).nodeChanged(selected);

            PreconditionRegistry.getInstance().save();
            ApplicationEventBus.getInstance().publish(new PreconditionSavedEvent());
            return;
        }


        PreconditionRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(new PreconditionSavedEvent());
    }

    /** Delete selected node: precondition or step. */
    public void deleteNode() {
        TestNode selected = (TestNode) precondTree.getLastSelectedPathComponent();
        if (selected == null || selected.getParent() == null) return;

        Object userObject = selected.getModelRef();
        Object parentObject = ((TestNode) selected.getParent()).getModelRef();

        if (userObject instanceof Precondition) {
            PreconditionRegistry.getInstance().getAll().remove(userObject);
        } else if (userObject instanceof TestAction && parentObject instanceof Precondition) {
            ((Precondition) parentObject).getActions().remove(userObject);
        }

        ((DefaultTreeModel) precondTree.getModel()).removeNodeFromParent(selected);
        ((DefaultTreeModel) precondTree.getModel()).nodeStructureChanged((TestNode) selected.getParent());

        PreconditionRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(new PreconditionSavedEvent());
    }

    // ===================== Helpers =====================

    private String safeName(String s) {
        return (s == null || s.trim().isEmpty()) ? "unnamed" : s.trim();
    }

    private String uniquePreconditionName(String base) {
        List<Precondition> pres = PreconditionRegistry.getInstance().getAll();
        Set<String> used = new HashSet<String>();
        for (Precondition p : pres) used.add(safeName(p.getName()));
        return makeUnique(base, used);
    }

    private String makeUnique(String base, Set<String> used) {
        String b = safeName(base);
        if (!used.contains(b)) return b;
        for (int i = 2; i < 10000; i++) {
            String cand = b + " (" + i + ")";
            if (!used.contains(cand)) return cand;
        }
        return b + " (copy)";
    }

    /** Clone TestAction shallow – copy known fields, optionally prefix name when present. */
    private TestAction cloneActionShallow(TestAction src, boolean tryPrefixName) {
        TestAction a = new TestAction();
        a.setType(src.getType());
        a.setAction(src.getAction());
        a.setValue(src.getValue());
        a.setUser(src.getUser());
        a.setTimeout(src.getTimeout());
        a.setSelectedSelector(src.getSelectedSelector());
        if (tryPrefixName) {
            String n = getNameIfExists(src);
            if (n != null && n.length() > 0) {
                setNameIfExists(a, "copy of " + n);
            }
        }
        return a;
    }

    private String getNameIfExists(Object bean) {
        try {
            java.lang.reflect.Method m = bean.getClass().getMethod("getName");
            Object v = m.invoke(bean);
            return (v != null) ? String.valueOf(v) : null;
        } catch (Exception ignore) { return null; }
    }
    private void setNameIfExists(Object bean, String name) {
        try {
            java.lang.reflect.Method m = bean.getClass().getMethod("setName", String.class);
            m.invoke(bean, name);
        } catch (Exception ignore) { /* no-op */ }
    }

    // Make node visible & selected
    private void selectNode(TestNode node) {
        DefaultTreeModel model = (DefaultTreeModel) precondTree.getModel();
        TreePath path = new TreePath(node.getPath());
        if (node.getParent() != null) {
            precondTree.expandPath(new TreePath(((DefaultMutableTreeNode) node.getParent()).getPath()));
        }
        precondTree.setSelectionPath(path);
        precondTree.scrollPathToVisible(path);
        model.nodeChanged(node);
    }
}
