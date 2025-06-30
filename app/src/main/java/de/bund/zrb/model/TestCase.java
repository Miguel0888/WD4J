package de.bund.zrb.model;

import java.util.ArrayList;
import java.util.List;

public class TestCase {
    private String id;
    private String name;
    private final List<TestAction> when = new ArrayList<>(); // ✅ Die Steps!

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<TestAction> getWhen() { return when; }
}
