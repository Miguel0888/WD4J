package de.bund.zrb.ui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.*;
import java.io.IOException;

/**
 * Enables true MOVE drag & drop for Testsuite JTree.
 */
public class TestSuiteTreeTransferHandler extends TransferHandler {

    // ==== DEBUG (einfach im Terminal lesen) ====
    private static final boolean DEBUG_DND = false;

    private static String typeOf(Object o) {
        if (o == null) return "ROOT";
        if (o instanceof de.bund.zrb.model.TestSuite) return "Suite";
        if (o instanceof de.bund.zrb.model.TestCase)  return "Case";
        if (o instanceof de.bund.zrb.model.TestAction) return "Action";
        return o.getClass().getSimpleName();
    }
    private static Object modelRefOf(DefaultMutableTreeNode n) {
        return (n instanceof TestNode) ? ((TestNode) n).getModelRef() : null;
    }
    private static String nodeInfo(DefaultMutableTreeNode n) {
        if (n == null) return "(null)";
        Object ref = modelRefOf(n);
        String txt = n.getUserObject() != null ? String.valueOf(n.getUserObject()) : "(no userObject)";
        return txt + " [" + typeOf(ref) + "]";
    }
    private static void log(String msg) {
        if (DEBUG_DND) System.out.println("[DND] " + msg);
    }

    /**
     * A custom DataFlavor used to transfer tree nodes within the same JVM.  Using
     * the {@link DataFlavor#javaJVMLocalObjectMimeType} prevents the node (and
     * its userObject) from being serialized during a drag operation.  Without
     * this, the default {@code application/x-java-serialized-object} MIME type
     * would be used, which attempts to serialize the entire tree node.  Since
     * {@link de.bund.zrb.model.TestAction} and some of the other model classes
     * do not implement {@link java.io.Serializable}, the drag operation would
     * fail with a {@link java.io.NotSerializableException}.  By using the JVM
     * local MIME type we ensure that objects are passed by reference within
     * this JVM and no serialization occurs.
     */
    private final DataFlavor nodesFlavor;

    /**
     * Keeps track of the node currently being dragged.  This reference is
     * cleared after the drop is handled so that exportDone does not attempt
     * to remove the node a second time.
     */
    private DefaultMutableTreeNode draggedNode; // Save reference for exportDone

    public TestSuiteTreeTransferHandler() {
        try {
            // Define a DataFlavor that uses the local JVM object MIME type to avoid serialization
            String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=" + DefaultMutableTreeNode.class.getName();
            nodesFlavor = new DataFlavor(mimeType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to create DataFlavor for JTree nodes", e);
        }
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        TreePath path = tree.getSelectionPath();
        if (path == null) return null;

        draggedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        return new NodesTransferable(draggedNode);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE; // MOVE only
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDrop()) return false;
        support.setShowDropLocation(true);
        return support.isDataFlavorSupported(nodesFlavor);
    }

    /**
     * Führt den Drop-Vorgang aus. Unterstützt sowohl ON-Drop (ohne Linie, Einfügen „nach Ziel“)
     * als auch INSERT-Drop (mit Einfügelinie, Einfügen an exakten childIndex).
     * <p>
     * Besondere Regeln:
     * <ul>
     *   <li>Root akzeptiert nur Suiten.</li>
     *   <li>Suite akzeptiert nur Cases.</li>
     *   <li>Case akzeptiert nur Actions (Steps).</li>
     *   <li>Bei INSERT zwischen zwei gleichartigen Eltern und gleichem Typ wird zwischen ihnen einsortiert,
     *       andernfalls (Kindtyp) als letztes Kind des linken (oberen) Parents.</li>
     * </ul>
     * Persistiert die neue Reihenfolge in {@code TestRegistry}.
     *
     * @param support Swing-TransferSupport vom Drop
     * @return {@code true} wenn der Move durchgeführt wurde, sonst {@code false}
     */
    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) return false;

        DropContext ctx = buildDropContext(support);
        if (!ctx.isValid()) return false;

        try {
            DefaultMutableTreeNode moved = getMovedNode(support);
            if (moved == null || moved.getParent() == null) return false; // Root selbst nie verschieben

            log("moved=" + nodeInfo(moved) + " | oldParent=" + nodeInfo((DefaultMutableTreeNode) moved.getParent()));

            // 1.) Erst den Move-Plan bestimmen (entscheidet Parent/Index je nach ON/INSERT & Typen)
            MovePlan plan = computeMovePlan(moved, ctx.dropTarget, ctx.childIndex);
            if (plan == null) {
                log("ABORT: computeMovePlan returned null (illegal combination)");
                return false;
            }
            if (plan.newParentNode == null) {
                log("ABORT: plan.newParentNode is null");
                return false;
            }
            if (plan.newParentNode == moved) {
                log("ABORT: newParent == moved");
                return false;
            }
            // Wichtig: KEIN moved.isNodeAncestor(plan.newParentNode) mehr – das führte bei INSERT zu False Positives.

            log("MovePlan: newParent=" + nodeInfo(plan.newParentNode) + " insertIndex=" + plan.insertIndex);

            // 2.) Off-by-one korrigieren, wenn innerhalb desselben Parents verschoben wird
            int beforeAdjust = plan.insertIndex;
            plan.insertIndex = adjustForSameParent((DefaultMutableTreeNode) moved.getParent(), moved, plan.insertIndex);
            if (beforeAdjust != plan.insertIndex) {
                log("adjustForSameParent: " + beforeAdjust + " -> " + plan.insertIndex);
            }

            // 3.) Domänenmodell aktualisieren (Listen in Case/Suite/Registry)
            applyDomainMove(moved, plan.newParentNode, plan.insertIndex);

            // 4.) Baummodell aktualisieren & Sichtbarkeit
            applyTreeMove(ctx.tree, moved, plan.newParentNode, plan.insertIndex);

            // 5.) Persistieren
            de.bund.zrb.service.TestRegistry.getInstance().save();

            // Verhindern, dass exportDone nochmals entfernt
            draggedNode = null;
            log("DONE");
            return true;
        } catch (UnsupportedFlavorException | IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /* ========================== Hilfsmethoden ========================== */

    private static final class DropContext {
        final JTree tree;
        final JTree.DropLocation dl;
        final TreePath destPath;
        final DefaultMutableTreeNode dropTarget;
        final int childIndex;

        DropContext(JTree tree, JTree.DropLocation dl, TreePath destPath,
                    DefaultMutableTreeNode dropTarget, int childIndex) {
            this.tree = tree;
            this.dl = dl;
            this.destPath = destPath;
            this.dropTarget = dropTarget;
            this.childIndex = childIndex;
        }
        boolean isValid() { return tree != null && dl != null && destPath != null && dropTarget != null; }
    }

    private static final class MovePlan {
        DefaultMutableTreeNode newParentNode;
        int insertIndex;
        MovePlan(DefaultMutableTreeNode parent, int index) { this.newParentNode = parent; this.insertIndex = index; }
    }

    private DropContext buildDropContext(TransferSupport support) {
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        TreePath destPath = dl != null ? dl.getPath() : null;
        JTree tree = (JTree) support.getComponent();
        DefaultMutableTreeNode dropTarget = destPath != null ? (DefaultMutableTreeNode) destPath.getLastPathComponent() : null;
        int childIndex = dl != null ? dl.getChildIndex() : -1;

        log("=== DROP ===");
        log("mode=" + (childIndex >= 0 ? "INSERT(line)" : "ON(node)"));
        log("path=" + (destPath != null ? destPath : "(null)"));
        log("dropTarget=" + nodeInfo(dropTarget));
        log("childIndex=" + childIndex);

        return new DropContext(tree, dl, destPath, dropTarget, childIndex);
    }

    private DefaultMutableTreeNode getMovedNode(TransferSupport support)
            throws UnsupportedFlavorException, IOException {
        return (DefaultMutableTreeNode) support.getTransferable().getTransferData(nodesFlavor);
    }

    private boolean isDropAllowed(DefaultMutableTreeNode moved, DefaultMutableTreeNode dropTarget) {
        // nicht in sich selbst / Nachfahren droppen
        if (moved.isNodeAncestor(dropTarget)) return false;
        return true;
    }

    private MovePlan computeMovePlan(DefaultMutableTreeNode moved,
                                     DefaultMutableTreeNode dropTarget,
                                     int childIndex) {
        // INSERT (Linie sichtbar): immer in den Parent (dropTarget) bei childIndex einfügen
        if (childIndex >= 0) {
            return planForInsert(moved, dropTarget, childIndex);
        }
        // ON (kein childIndex): „nach Ziel“ bzw. „ans Ende“ je nach Typen
        return planForOnDrop(moved, dropTarget);
    }

    /**
     * Erzeugt einen {@link MovePlan} für Drop‐Vorgänge mit Einfügelinie (childIndex >= 0).
     * Wird zwischen zwei Knoten des selben Eltern-Typs gedropped und der verschobene Knoten
     * hat ebenfalls diesen Typ, erfolgt eine echte Umsortierung zwischen den Eltern.
     * Andernfalls wird der verschobene Knoten als letztes Kind des linken („oberen“) Parents angefügt.
     *
     * @param moved      der verschobene Knoten
     * @param dropTarget der Drop-Pfad (immer der Elternknoten, nicht das Element dahinter)
     * @param childIndex Einfügeposition laut DropLocation (0 … childCount)
     * @return ein MovePlan mit neuem Parent und Einfügeindex, oder {@code null} bei unerlaubtem Drop
     */
    private MovePlan planForInsert(DefaultMutableTreeNode moved,
                                   DefaultMutableTreeNode dropTarget,
                                   int childIndex) {
        Object movedObj = modelRefOf(moved);

        int siblingsCount = dropTarget != null ? dropTarget.getChildCount() : -1;
        DefaultMutableTreeNode prevSibling = (dropTarget != null && childIndex > 0)
                ? (DefaultMutableTreeNode) dropTarget.getChildAt(childIndex - 1) : null;
        DefaultMutableTreeNode nextSibling = (dropTarget != null && childIndex >= 0 && childIndex < siblingsCount)
                ? (DefaultMutableTreeNode) dropTarget.getChildAt(childIndex) : null;

        log("INSERT plan: dropTarget=" + nodeInfo(dropTarget)
                + " childIndex=" + childIndex
                + " prev=" + nodeInfo(prevSibling)
                + " next=" + nodeInfo(nextSibling)
                + " movedType=" + typeOf(movedObj));

        // 1) Linie zwischen zwei gleichartigen Eltern (z. B. Suite|Suite, Case|Case)
        if (prevSibling != null && nextSibling != null) {
            Object prevRef = modelRefOf(prevSibling);
            Object nextRef = modelRefOf(nextSibling);

            if (prevRef != null && nextRef != null && prevRef.getClass().equals(nextRef.getClass())) {
                Class<?> parentType = prevRef.getClass();

                if (movedObj != null && parentType.equals(movedObj.getClass())) {
                    // gleicher Typ → echte Umsortierung zwischen prev/next
                    log("→ Einsortierung zwischen gleichartigen Eltern, gleicher Typ → zwischen prev/next");
                    return new MovePlan(dropTarget, childIndex);
                } else {
                    // Kind-/Enkel-Typ → an das ENDE des zulässigen Containers unterhalb des linken Parents
                    DefaultMutableTreeNode legalParent = resolveLegalParentForChild(prevSibling, movedObj);
                    if (legalParent != null) {
                        int idx = legalParent.getChildCount();
                        log("→ Kind-/Enkeltyp → an ENDE des zulässigen Parents: " + nodeInfo(legalParent) + " @ " + idx);
                        return new MovePlan(legalParent, idx);
                    } else {
                        log("→ Kein zulässiger Parent unter linkem Parent gefunden → DROP unzulässig");
                        return null;
                    }
                }
            }
        }

        // 2) Standard (Drop auf einen konkreten Parent): ROOT→Suite, Suite→Case, Case→Action
        Object parentObj = modelRefOf(dropTarget);

        if (parentObj == null) { // ROOT akzeptiert nur Suites
            boolean ok = (movedObj instanceof de.bund.zrb.model.TestSuite);
            log("ROOT insert ok=" + ok);
            return ok ? new MovePlan(dropTarget, childIndex) : null;
        }
        if (parentObj instanceof de.bund.zrb.model.TestSuite) { // Suite akzeptiert nur Cases
            boolean ok = (movedObj instanceof de.bund.zrb.model.TestCase);
            log("SUITE insert ok=" + ok);
            return ok ? new MovePlan(dropTarget, childIndex) : null;
        }
        if (parentObj instanceof de.bund.zrb.model.TestCase) { // Case akzeptiert nur Actions
            boolean ok = (movedObj instanceof de.bund.zrb.model.TestAction);
            log("CASE insert ok=" + ok);
            return ok ? new MovePlan(dropTarget, childIndex) : null;
        }

        log("→ Parent-Typ nicht zulässig für INSERT");
        return null;
    }

    private MovePlan planForOnDrop(DefaultMutableTreeNode moved,
                                   DefaultMutableTreeNode dropTarget) {
        Object movedObj = modelRefOf(moved);
        Object dropObj  = modelRefOf(dropTarget);

        log("ON plan: target=" + nodeInfo(dropTarget) + " moved=" + nodeInfo(moved));

        // Action …
        if (movedObj instanceof de.bund.zrb.model.TestAction) {
            if (dropObj instanceof de.bund.zrb.model.TestCase) {
                log("→ Action ON Case → append to Case");
                return new MovePlan(dropTarget, dropTarget.getChildCount());
            } else if (dropObj instanceof de.bund.zrb.model.TestAction) {
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dropTarget.getParent();
                if (parent == null) return null;
                log("→ Action ON Action → after target within parent");
                return new MovePlan(parent, parent.getIndex(dropTarget) + 1);
            }
            log("→ Action ON invalid target");
            return null;
        }

        // Case …
        if (movedObj instanceof de.bund.zrb.model.TestCase) {
            if (dropObj instanceof de.bund.zrb.model.TestSuite) {
                log("→ Case ON Suite → append to Suite");
                return new MovePlan(dropTarget, dropTarget.getChildCount());
            } else if (dropObj instanceof de.bund.zrb.model.TestCase) {
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dropTarget.getParent();
                if (parent == null) return null;
                log("→ Case ON Case → after target within Suite");
                return new MovePlan(parent, parent.getIndex(dropTarget) + 1);
            }
            log("→ Case ON invalid target");
            return null;
        }

        // Suite …
        if (movedObj instanceof de.bund.zrb.model.TestSuite) {
            if (dropObj == null) {
                log("→ Suite ON ROOT → append to ROOT");
                return new MovePlan(dropTarget, dropTarget.getChildCount());
            } else if (dropObj instanceof de.bund.zrb.model.TestSuite) {
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dropTarget.getParent();
                if (parent == null) return null;
                log("→ Suite ON Suite → after target in ROOT");
                return new MovePlan(parent, parent.getIndex(dropTarget) + 1);
            }
            log("→ Suite ON invalid target");
            return null;
        }

        log("→ ON: unknown moved type");
        return null;
    }

    private int adjustForSameParent(DefaultMutableTreeNode oldParent,
                                    DefaultMutableTreeNode moved,
                                    int insertIndex) {
        if (oldParent == null) return insertIndex;
        int oldIndex = oldParent.getIndex(moved);
        if (oldIndex >= 0 && insertIndex > oldIndex) {
            return insertIndex - 1;
        }
        return insertIndex;
    }

    private void applyDomainMove(DefaultMutableTreeNode moved,
                                 DefaultMutableTreeNode newParentNode,
                                 int insertIndex) {
        Object movedObj = modelRefOf(moved);
        DefaultMutableTreeNode oldParentNode = (DefaultMutableTreeNode) moved.getParent();
        Object oldParentObj = modelRefOf(oldParentNode);
        Object newParentObj = modelRefOf(newParentNode);

        log("DomainMove: " + typeOf(movedObj) + " " + nodeInfo(moved)
                + "  oldParent=" + nodeInfo(oldParentNode)
                + "  newParent=" + nodeInfo(newParentNode)
                + "  insertIndex=" + insertIndex);

        if (movedObj instanceof de.bund.zrb.model.TestAction) {
            de.bund.zrb.model.TestAction action = (de.bund.zrb.model.TestAction) movedObj;

            if (oldParentObj instanceof de.bund.zrb.model.TestCase) {
                de.bund.zrb.model.TestCase oldCase = (de.bund.zrb.model.TestCase) oldParentObj;
                oldCase.getWhen().remove(action);
            }
            if (newParentObj instanceof de.bund.zrb.model.TestCase) {
                de.bund.zrb.model.TestCase newCase = (de.bund.zrb.model.TestCase) newParentObj;
                java.util.List<de.bund.zrb.model.TestAction> list = newCase.getWhen();
                int idx = clamp(insertIndex, list.size());
                list.add(idx, action);

                // Fix parentId to keep domain graph consistent
                action.setParentId(newCase.getId());
            }
            return;
        }

        if (movedObj instanceof de.bund.zrb.model.TestCase) {
            de.bund.zrb.model.TestCase tc = (de.bund.zrb.model.TestCase) movedObj;

            if (oldParentObj instanceof de.bund.zrb.model.TestSuite) {
                de.bund.zrb.model.TestSuite oldSuite = (de.bund.zrb.model.TestSuite) oldParentObj;
                oldSuite.getTestCases().remove(tc);
            }
            if (newParentObj instanceof de.bund.zrb.model.TestSuite) {
                de.bund.zrb.model.TestSuite newSuite = (de.bund.zrb.model.TestSuite) newParentObj;
                java.util.List<de.bund.zrb.model.TestCase> list = newSuite.getTestCases();
                int idx = clamp(insertIndex, list.size());
                list.add(idx, tc);

                // Fix parentId to keep domain graph consistent
                tc.setParentId(newSuite.getId());
            }
            return;
        }

        if (movedObj instanceof de.bund.zrb.model.TestSuite) {
            de.bund.zrb.model.TestSuite suite = (de.bund.zrb.model.TestSuite) movedObj;
            java.util.List<de.bund.zrb.model.TestSuite> list = de.bund.zrb.service.TestRegistry.getInstance().getAll();
            list.remove(suite);
            list.add(clamp(insertIndex, list.size()), suite);
        }
    }

    private int clamp(int idx, int size) {
        if (idx < 0) return 0;
        if (idx > size) return size;
        return idx;
    }

    private void applyTreeMove(JTree tree,
                               DefaultMutableTreeNode moved,
                               DefaultMutableTreeNode newParentNode,
                               int insertIndex) {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

        log("TreeMove: removing from " + nodeInfo((DefaultMutableTreeNode) moved.getParent()));
        model.removeNodeFromParent(moved);

        insertIndex = Math.max(0, Math.min(insertIndex, newParentNode.getChildCount()));
        log("TreeMove: inserting into " + nodeInfo(newParentNode) + " at " + insertIndex);

        model.insertNodeInto(moved, newParentNode, insertIndex);

        TreePath newPath = new TreePath(moved.getPath());
        tree.expandPath(new TreePath(newParentNode.getPath()));
        tree.setSelectionPath(newPath);
        tree.scrollPathToVisible(newPath);
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        // Removal is handled in importData.  Only remove if the drag was aborted.
        if (action == MOVE && draggedNode != null) {
            JTree tree = (JTree) source;
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.removeNodeFromParent(draggedNode);
        }
        draggedNode = null;
    }

    private static class NodesTransferable implements Transferable {
        private final DefaultMutableTreeNode node;
        private final DataFlavor[] flavors;

        public NodesTransferable(DefaultMutableTreeNode node) {
            this.node = node;
            // Use the same JVM-local DataFlavor as the handler to avoid serialization
            String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=" + DefaultMutableTreeNode.class.getName();
            try {
                this.flavors = new DataFlavor[]{new DataFlavor(mimeType)};
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to create DataFlavor for Transferable", e);
            }
        }

        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavors[0].equals(flavor);
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return node;
        }
    }

    /**
     * Ermittelt den „rechtlich“ zulässigen Parent unterhalb von leftParentOrSibling,
     * in den ein movedObj eingefügt werden darf:
     * - Action → Case: wenn left = Case → left; wenn left = Suite → letzter Case der Suite.
     * - Case   → Suite: wenn left = Suite → Suite; wenn left = Case → dessen Parent (Suite).
     * - Suite  → ROOT:  wenn left = Suite → ROOT bleibt der Parent (wird über dropTarget/childIndex geregelt).
     * Liefert null, wenn kein erlaubter Ziel-Parent existiert (z. B. Suite ohne Cases für eine Action).
     */
    private DefaultMutableTreeNode resolveLegalParentForChild(DefaultMutableTreeNode leftParentOrSibling, Object movedObj) {
        Object leftRef = modelRefOf(leftParentOrSibling);

        if (movedObj instanceof de.bund.zrb.model.TestAction) {
            // Action braucht einen Case
            if (leftRef instanceof de.bund.zrb.model.TestCase) {
                return leftParentOrSibling;
            }
            if (leftRef instanceof de.bund.zrb.model.TestSuite) {
                DefaultMutableTreeNode lastCase = lastChildOfType(leftParentOrSibling, de.bund.zrb.model.TestCase.class);
                return lastCase; // kann null sein → kein Case vorhanden → Drop unzulässig
            }
            return null;
        }

        if (movedObj instanceof de.bund.zrb.model.TestCase) {
            // Case braucht eine Suite
            if (leftRef instanceof de.bund.zrb.model.TestSuite) {
                return leftParentOrSibling;
            }
            if (leftRef instanceof de.bund.zrb.model.TestCase) {
                // zwischen zwei Cases → Parent ist deren Suite
                return (DefaultMutableTreeNode) leftParentOrSibling.getParent();
            }
            return null;
        }

        if (movedObj instanceof de.bund.zrb.model.TestSuite) {
            // Suites werden über ROOT einsortiert – hier keine Absenkung nötig
            return (DefaultMutableTreeNode) leftParentOrSibling.getParent(); // i. d. R. ROOT
        }

        return null;
    }

    /** Liefert das letzte Kind eines bestimmten ModelRef-Typs unterhalb des gegebenen Parent-Knotens. */
    private DefaultMutableTreeNode lastChildOfType(DefaultMutableTreeNode parent, Class<?> modelType) {
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            DefaultMutableTreeNode c = (DefaultMutableTreeNode) parent.getChildAt(i);
            Object ref = modelRefOf(c);
            if (ref != null && modelType.isAssignableFrom(ref.getClass())) {
                return c;
            }
        }
        return null;
    }

}
