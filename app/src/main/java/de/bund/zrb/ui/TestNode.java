package de.bund.zrb.ui;

import de.bund.zrb.model.TestAction;

import javax.swing.tree.DefaultMutableTreeNode;

public class TestNode extends DefaultMutableTreeNode {

    public enum Status { UNDEFINED, PASSED, FAILED }

    private Status status = Status.UNDEFINED;

    private final Object modelRef;

    public TestNode(String name) {
        this(name, null);
    }

    public TestNode(String name, Object modelRef) {
        super(name);
        this.modelRef = modelRef;
    }

    public Object getModelRef() {
        return modelRef;
    }

    public TestAction getAction() {
        return modelRef instanceof TestAction ? (TestAction) modelRef : null;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return getUserObject().toString();
    }
}
