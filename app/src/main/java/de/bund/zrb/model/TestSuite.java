package de.bund.zrb.model;

import java.util.ArrayList;
import java.util.List;

public class TestSuite {
    private String id;
    private String name;
    private final List<TestCase> testCases = new ArrayList<>();
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<TestCase> getTestCases() { return testCases; }
}
