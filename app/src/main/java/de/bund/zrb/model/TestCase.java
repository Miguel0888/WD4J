package de.bund.zrb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestCase {

    private String id;
    private String parentId; // verweist auf TestSuite.id
    private String name;

    private List<GivenCondition> given = new ArrayList<GivenCondition>();
    private List<TestAction> when = new ArrayList<TestAction>();
    private List<ThenExpectation> then = new ArrayList<ThenExpectation>();

    public TestCase() {
        this.id = UUID.randomUUID().toString();
    }

    public TestCase(String name, List<TestAction> whenSteps) {
        this();
        this.name = name;
        if (whenSteps != null) {
            this.when.addAll(whenSteps);
        }
    }

    // --- IDs ---
    public String getId() { return id; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    // --- Name ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // --- Given/When/Then ---
    public List<GivenCondition> getGiven() { return given; }
    public List<TestAction> getWhen() { return when; }
    public List<ThenExpectation> getThen() { return then; }

    public void setId(String id) {
        this.id = id;
    }
}
