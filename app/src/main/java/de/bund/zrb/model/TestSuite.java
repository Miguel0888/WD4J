package de.bund.zrb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A logical test suite in the tree.
 *
 * Hierarchie:
 * - parentId verweist auf die RootNode.id
 * - testCases gehören zu dieser Suite
 *
 * Inhalt:
 * - given / then auf Suite-Ebene (bestehendes Verhalten bleibt)
 * - description bleibt als freier Beschreibungstext für den Nutzer erhalten
 *
 * IDs:
 * - Jede Suite hat eine eigene UUID (id).
 * - parentId wird vom Registry/Repair gesetzt,
 *   wenn die Suite in den Root-Baum gehängt wird.
 */
public class TestSuite {
    private String id;
    private String parentId;

    private String name;
    private String description;

    private final List<GivenCondition> given = new ArrayList<>();
    private final List<ThenExpectation> then = new ArrayList<>();
    private final List<TestCase> testCases = new ArrayList<>();

    // Suite-spezifische Scopes:
    // - beforeAll: Variablen, die EINMAL vor der Suite evaluiert werden
    // - beforeEach: Variablen, die VOR JEDEM TestCase evaluiert werden
    // - templates: Funktionszeiger (lazy), die später im When dereferenziert werden
    private final List<GivenCondition> beforeAll = new ArrayList<>();
    private final List<GivenCondition> beforeEach = new ArrayList<>();
    private final List<GivenCondition> templates = new ArrayList<>();

    public TestSuite() {
        // leer für Gson
    }

    public TestSuite(String name, List<TestCase> testCases) {
        this.name = name;
        if (testCases != null) {
            this.testCases.addAll(testCases);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<GivenCondition> getGiven() { return given; }
    public List<ThenExpectation> getThen() { return then; }
    public List<TestCase> getTestCases() { return testCases; }

    public List<GivenCondition> getBeforeAll() {
        return beforeAll;
    }

    public List<GivenCondition> getBeforeEach() {
        return beforeEach;
    }

    public List<GivenCondition> getTemplates() {
        return templates;
    }
}

