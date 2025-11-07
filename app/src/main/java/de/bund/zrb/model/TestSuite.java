package de.bund.zrb.model;

import de.bund.zrb.runtime.ExpressionRegistry;

import java.util.*;

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

    private final List<ThenExpectation> then = new ArrayList<>();
    private final List<TestCase> testCases = new ArrayList<>();

    private final java.util.Map<String,String> beforeAll   = new java.util.LinkedHashMap<>();
    private final Map<String, Boolean> beforeAllEnabled    = new HashMap<>();
    private final java.util.Map<String,String> beforeEach  = new java.util.LinkedHashMap<>();
    private final Map<String, Boolean> beforeEachEnabled   = new HashMap<>();
    private final java.util.Map<String,String> templates   = new java.util.LinkedHashMap<>();
    private final Map<String, Boolean> templatesEnabled    = new HashMap<>();

    private final java.util.Map<String,String> afterAll   = new java.util.LinkedHashMap<>();
    private final Map<String, Boolean> afterAllEnabled = new HashMap<>();
    private final Map<String, String> afterAllDesc = new HashMap<>();

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

    public List<ThenExpectation> getThen() { return then; }
    public List<TestCase> getTestCases() { return testCases; }


    public Map<String,String> getBeforeAll()   { return beforeAll; }
    public Map<String, Boolean> getBeforeAllEnabled() { return beforeAllEnabled; }
    public Map<String,String> getBeforeEach()  { return beforeEach; }
    public Map<String, Boolean> getBeforeEachEnabled() { return beforeEachEnabled; }
    public Map<String,String> getTemplates()   { return templates; }
    public Map<String, Boolean> getTemplatesEnabled() { return templatesEnabled; }

    public Map<String, String> getAfterAll() {
        return afterAll;
    }

    public Map<String, Boolean> getAfterAllEnabled() {
        return afterAllEnabled;
    }

    public Map<String, String> getAfterAllDesc() {
        return afterAllDesc;
    }
}

