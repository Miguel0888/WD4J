package de.bund.zrb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestCase {
    private String id;
    private String parentId;

    private String name;

    private final List<GivenCondition> given = new ArrayList<>();
    private final List<TestAction> when = new ArrayList<>();
    private final List<ThenExpectation> then = new ArrayList<>();

    // NEU:
    // Case hat nur EIN "Before"-Scope:
    private final List<ScopeVariableEntry> beforeVars      = new ArrayList<>();
    private final List<ScopeTemplateEntry> templates       = new ArrayList<>();

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

    public List<GivenCondition> getGiven() { return given; }
    public List<TestAction> getWhen() { return when; }
    public List<ThenExpectation> getThen() { return then; }

    // NEU Getter:
    public List<ScopeVariableEntry> getBeforeVars() { return beforeVars; }
    public List<ScopeTemplateEntry> getTemplates() { return templates; }
}

