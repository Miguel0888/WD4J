package de.bund.zrb.model;

import java.util.ArrayList;
import java.util.List;

public class TestCase {
    private String id;
    private String parentId;

    private String name;


    private final List<TestAction> when = new ArrayList<>();
    private final List<ThenExpectation> then = new ArrayList<>();

    private final List<GivenCondition> before = new ArrayList<>();
    private final List<GivenCondition> templates = new ArrayList<>();

    public TestCase() {}

    public TestCase(String name, List<TestAction> when) {
        this.name = name;
        if (when != null) {
            this.when.addAll(when);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<GivenCondition> getBefore() { return before; }
    public List<TestAction> getWhen() { return when; }
    public List<ThenExpectation> getThen() { return then; }

    public List<GivenCondition> getTemplates() {
        return templates;
    }
}

