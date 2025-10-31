package de.bund.zrb.ui.expressions;

import de.bund.zrb.model.*;
import de.bund.zrb.service.TestRegistry;

import java.util.LinkedHashMap;
import java.util.List;

public class GivenLookupService {

    private final TestRegistry testRegistry = TestRegistry.getInstance();

    public ScopeData collectForAction(TestAction action) {
        // 1. Action -> Case -> Suite -> Root
        TestCase tc = testRegistry.findCaseById(action.getParentId());
        TestSuite suite = (tc != null) ? testRegistry.findSuiteById(tc.getParentId()) : null;
        RootNode root = testRegistry.getRoot();

        // 2. Variablen einsammeln
        LinkedHashMap<String, GivenCondition> vars = new LinkedHashMap<>();
        if (root != null) {
            takeVars(vars, root.getBeforeEach()); // Root.beforeEach
        }
        if (suite != null) {
            takeVars(vars, suite.getBeforeEach()); // Suite.beforeEach
        }
        if (tc != null) {
            takeVars(vars, tc.getBeforeCase()); // Case.beforeCase
        }

        // 3. Templates einsammeln
        LinkedHashMap<String, GivenCondition> temps = new LinkedHashMap<>();
        if (root != null) {
            takeTemps(temps, root.getTemplates());
        }
        if (suite != null) {
            takeTemps(temps, suite.getTemplates());
        }
        if (tc != null) {
            takeTemps(temps, tc.getTemplates());
        }

        return new ScopeData(vars, temps);
    }

    private void takeVars(LinkedHashMap<String, GivenCondition> map, List<GivenCondition> list) {
        // iterate from "closest" to "farther away" in caller order,
        // first definition wins:
        // parseValueMap to get "name"
        // if not yet in map -> put
    }

    private void takeTemps(LinkedHashMap<String, GivenCondition> map, List<GivenCondition> list) {
        // same Prinzip wie oben
    }

    public static class ScopeData {
        public final LinkedHashMap<String, GivenCondition> variables; // username -> GivenCondition
        public final LinkedHashMap<String, GivenCondition> templates; // otpCode -> GivenCondition

        public ScopeData(
                LinkedHashMap<String, GivenCondition> variables,
                LinkedHashMap<String, GivenCondition> templates
        ) {
            this.variables = variables;
            this.templates = templates;
        }
    }
}
