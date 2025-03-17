package app.dto;

import java.util.List;

public class TestCase {
    private String name;
    private List<TestAction> given;
    private List<TestAction> when;
    private List<TestAction> then;

    public TestAction[] getGiven() {
        return given.toArray(new TestAction[0]);
    }

    public TestAction[] getWhen() {
        return when.toArray(new TestAction[0]);
    }

    public TestAction[] getThen() {
        return then.toArray(new TestAction[0]);
    }

    public void setGiven(List<TestAction> givenActions) {
        this.given = givenActions;
    }

    public void setWhen(List<TestAction> whenActions) {
        this.when = whenActions;
    }

    public void setThen(List<TestAction> thenActions) {
        this.then = thenActions;
    }

    public void setName(String string) {
        this.name = string;
    }
}
