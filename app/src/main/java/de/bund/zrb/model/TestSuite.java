package de.bund.zrb.model;

import java.util.List;

public class TestSuite {
    private String id;
    private String name;
    private List<TestCase> testCases;

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCase> testCases) {
        this.testCases = testCases;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String suiteName) {
        this.name = suiteName;
    }
}