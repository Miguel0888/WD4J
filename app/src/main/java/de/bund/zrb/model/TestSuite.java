package de.bund.zrb.model;

import java.util.ArrayList;
import java.util.List;

public class TestSuite {
    private String id;
    private String name;

    private final List<GivenCondition> given = new ArrayList<>();
    private final List<ThenExpectation> then = new ArrayList<>();
    private final List<TestCase> testCases = new ArrayList<>();

    public TestSuite(String name, List<TestCase> testCases) {
        this.name = name;
        this.testCases.addAll(testCases);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<GivenCondition> getGiven() { return given; }
    public List<ThenExpectation> getThen() { return then; }
    public List<TestCase> getTestCases() { return testCases; }
}
