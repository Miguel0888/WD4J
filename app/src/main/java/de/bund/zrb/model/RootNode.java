package de.bund.zrb.model;

import de.bund.zrb.runtime.ExpressionRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RootNode {

    private String id;

    private List<TestSuite> testSuites = new ArrayList<>();

    // Variablen, die EINMAL berechnet werden (global einmalig)
    private final java.util.Map<String,String> beforeAll    = new java.util.LinkedHashMap<>();

    // Variablen, die vor JEDEM Case gesetzt werden sollen (globaler Default)
    private final java.util.Map<String,String> beforeEach   = new java.util.LinkedHashMap<>();

    // Templates = Funktionshandles (lazy, z.B. "otpCode" -> "otpCode({{username}})")
    private final java.util.Map<String,String> templates    = new java.util.LinkedHashMap<>();

    // Variablen, die nach JEDEM Case gesetzt werden sollen (globaler Default)
    private final java.util.Map<String,String> afterEach   = new java.util.LinkedHashMap<>();
    private final Map<String, Boolean> afterEachEnabled = new HashMap<>();
    private final Map<String,String> afterEachDesc = new HashMap<>();

    public RootNode() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; } // für Gson / Migration

    public List<TestSuite> getTestSuites() { return testSuites; }
    public void setTestSuites(List<TestSuite> testSuites) {
        this.testSuites = (testSuites != null) ? testSuites : new ArrayList<TestSuite>();
    }

    public Map<String, String> getBeforeAll() {
        return beforeAll;
    }

    public Map<String, String> getBeforeEach() {
        return beforeEach;
    }

    public Map<String, String> getTemplates() {
        return templates;
    }

    public Map<String, String> getAfterEach() {
        return afterEach;
    }

    public Map<String, Boolean> getAfterEachEnabled() {
        return afterEachEnabled;
    }

    public Map<String, String> getAfterEachDesc() {
        return afterEachDesc;
    }
}

