package de.bund.zrb.model;

import java.util.ArrayList;
import java.util.List;

public class RootNode {

    private String id;

    // existiert schon:
    private List<TestSuite> testSuites = new ArrayList<>();

    // NEU:
    private final List<GivenCondition> beforeAllVars   = new ArrayList<>();
    private final List<GivenCondition> beforeEachVars  = new ArrayList<>();
    private final List<GivenCondition> templates       = new ArrayList<>();

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
    public List<GivenCondition> getBeforeAll() { return beforeAllVars; }
    public List<GivenCondition> getBeforeEach() { return beforeEachVars; }
    public List<GivenCondition> getTemplates() { return templates; }

}

