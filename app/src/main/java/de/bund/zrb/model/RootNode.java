package de.bund.zrb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RootNode {

    private String id;
    private List<TestSuite> testSuites = new ArrayList<TestSuite>();

    public RootNode() {
        this.id = UUID.randomUUID().toString();
    }

    public RootNode(String id) {
        this.id = (id != null ? id : UUID.randomUUID().toString());
    }

    public String getId() {
        return id;
    }

    public List<TestSuite> getTestSuites() {
        return testSuites;
    }

    public void setTestSuites(List<TestSuite> suites) {
        this.testSuites = (suites != null ? suites : new ArrayList<TestSuite>());
    }

    public void addSuite(TestSuite suite) {
        if (suite != null) {
            testSuites.add(suite);
        }
    }
}
