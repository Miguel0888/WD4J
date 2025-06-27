package de.bund.zrb.ui;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Represents a test or suite node with status.
 */
public class TestNode extends DefaultMutableTreeNode {

    public enum Status {
        UNDEFINED, PASSED, FAILED
    }

    private Status status = Status.UNDEFINED;

    public TestNode(String name) {
        super(name);
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
