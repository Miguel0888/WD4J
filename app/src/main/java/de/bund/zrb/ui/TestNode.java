package de.bund.zrb.ui;

import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;

import javax.swing.tree.DefaultMutableTreeNode;

public class TestNode extends DefaultMutableTreeNode {

    public enum Status { UNDEFINED, PASSED, FAILED }

    private Status status = Status.UNDEFINED;
    private final TestAction action;

    public TestNode(String name) {
        this(name, null);
    }

    public TestNode(String name, TestAction action) {
        super(name);
        this.action = action;
    }

    public TestNode(TestSuite suite) {
        super(suite);
        this.action = null;
    }

    public TestNode(TestCase testCase) {
        super(testCase);
        this.action = null;
    }

    public TestNode(TestAction action) {
        super(action);
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
        Object obj = getUserObject();
        if (obj instanceof TestSuite) {
            return ((TestSuite) obj).getName();
        } else if (obj instanceof TestCase) {
            return ((TestCase) obj).getName();
        } else if (obj instanceof TestAction) {
            TestAction a = (TestAction) obj;
            String label = a.getAction();
            if (a.getValue() != null && !a.getValue().isEmpty()) {
                label += " [" + a.getValue() + "]";
            } else if (a.getSelectedSelector() != null) {
                label += " [" + a.getSelectedSelector() + "]";
            }
            return label;
        } else {
            return super.toString();
        }
    }
}
