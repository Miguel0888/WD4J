package de.bund.zrb.ui;

import de.bund.zrb.model.TestAction;

import javax.swing.tree.DefaultMutableTreeNode;

public class TestNode extends DefaultMutableTreeNode {

    public enum Status { UNDEFINED, PASSED, FAILED }

    private Status status = Status.UNDEFINED;

    private final TestAction action; // 💥

    public TestNode(String name) {
        this(name, null);
    }

    public TestNode(String name, TestAction action) {
        super(name);
        this.action = action;
    }

    public TestAction getAction() {
        return action;
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
