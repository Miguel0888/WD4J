package de.bund.zrb.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class TestCase {
    private String name;
    private List<TestAction> given;
    private List<TestAction> when;
    private List<TestAction> then;

    public List<TestAction> getGiven() {
        return given;  // Direkt eine Liste zurückgeben
    }

    public List<TestAction> getWhen() {
        return when;  // Direkt eine Liste zurückgeben
    }

    public List<TestAction> getThen() {
        return then;  // Direkt eine Liste zurückgeben
    }

    public void setGiven(List<TestAction> givenActions) {
        this.given = givenActions;
    }

    public void setWhen(List<TestAction> whenActions) {
        this.when = whenActions;
    }

    public void setThen(List<TestAction> thenActions) {
        this.then = thenActions;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public JsonObject toPlaywrightJson() {
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();
        for (TestAction action : steps) {
            array.add(action.toPlaywrightJson());
        }
        root.add("steps", array);
        return root;
    }
}
