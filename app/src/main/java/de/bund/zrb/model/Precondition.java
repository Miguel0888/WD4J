package de.bund.zrb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represent a reusable setup sequence (Precondition) that can be referenced by tests.
 * Do not mix test assertions here; this is purely Given/When for state preparation.
 */
public class Precondition {

    private String id;                    // UUID string
    private String name;                  // Human-readable name
    private List<Precondtion> given;   // May contain precond-ref items; no self-reference
    private List<TestAction> actions;     // When-steps of the precondition

    public Precondition() {
        this.given = new ArrayList<Precondtion>();
        this.actions = new ArrayList<TestAction>();
    }

    public Precondition(String id, String name) {
        this();
        this.id = id;
        this.name = name;
    }

    // ---------- Getters / Setters ----------

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public List<Precondtion> getGiven() { return given; }

    public void setGiven(List<Precondtion> given) {
        this.given = (given != null) ? given : new ArrayList<Precondtion>();
    }

    public List<TestAction> getActions() { return actions; }

    public void setActions(List<TestAction> actions) {
        this.actions = (actions != null) ? actions : new ArrayList<TestAction>();
    }

    // ---------- Convenience ----------

    /** Return true if this precondition has no steps and no referenced givens. */
    public boolean isEmpty() {
        return (given == null || given.isEmpty()) && (actions == null || actions.isEmpty());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Precondition)) return false;
        Precondition that = (Precondition) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return id != null ? id.hashCode() : 0; }

    @Override
    public String toString() {
        return "Precondition{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", given=" + (given != null ? given.size() : 0) +
                ", actions=" + (actions != null ? actions.size() : 0) +
                '}';
    }
}
