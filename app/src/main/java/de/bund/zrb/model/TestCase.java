package de.bund.zrb.model;

import de.bund.zrb.runtime.ExpressionRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestCase {
    private String id;
    private String parentId;

    private String name;


    private final List<TestAction> when = new ArrayList<>();
    private final List<ThenExpectation> then = new ArrayList<>();

    private final java.util.Map<String,String> before    = new java.util.LinkedHashMap<>();
    private final java.util.Map<String,String> templates = new java.util.LinkedHashMap<>();

    private final java.util.Map<String,String> after    = new java.util.LinkedHashMap<>();
    private final Map<String, Boolean> afterEnabled = new HashMap<>();
    private final Map<String, String> afterDesc = new HashMap<>();

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

    public List<TestAction> getWhen() { return when; }
    public List<ThenExpectation> getThen() { return then; }

    public java.util.Map<String,String> getBefore()    { return before; }
    public java.util.Map<String,String> getTemplates() { return templates; }

    public Map<String, String> getAfter() {
        return after;
    }

    public Map<String, Boolean> getAfterEnabled() {
        return afterEnabled;
    }

    public Map<String, String> getAfterDesc() {
        return afterDesc;
    }
}

