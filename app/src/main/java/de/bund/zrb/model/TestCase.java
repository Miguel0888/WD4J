package de.bund.zrb.model;

import java.util.ArrayList;
import java.util.List;

public class TestCase {

    private String id;
    private String name;

    private final List<GivenCondition> given = new ArrayList<>(); // ✅ Given
    private final List<TestAction> when = new ArrayList<>();      // ✅ Steps / When
    private final List<ThenExpectation> then = new ArrayList<>(); // ✅ Erwartungen / Then

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<GivenCondition> getGiven() { return given; }
    public List<TestAction> getWhen() { return when; }
    public List<ThenExpectation> getThen() { return then; }
}

