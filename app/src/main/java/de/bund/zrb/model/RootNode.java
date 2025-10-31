package de.bund.zrb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RootNode {

    private String id;

    // existiert schon:
    private List<TestSuite> testSuites = new ArrayList<>();

    // NEU:
    private final List<ScopeVariableEntry> beforeAllVars   = new ArrayList<>();
    private final List<ScopeVariableEntry> beforeEachVars  = new ArrayList<>();
    private final List<ScopeTemplateEntry> templates       = new ArrayList<>();

    public RootNode() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; } // für Gson / Migration

    public List<TestSuite> getTestSuites() { return testSuites; }
    public void setTestSuites(List<TestSuite> testSuites) {
        this.testSuites = (testSuites != null) ? testSuites : new ArrayList<TestSuite>();
    }

    // NEU Getter
    public List<ScopeVariableEntry> getBeforeAllVars() { return beforeAllVars; }
    public List<ScopeVariableEntry> getBeforeEachVars() { return beforeEachVars; }
    public List<ScopeTemplateEntry> getTemplates() { return templates; }
}

