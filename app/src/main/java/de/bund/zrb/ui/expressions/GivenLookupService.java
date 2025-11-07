package de.bund.zrb.ui.expressions;

import de.bund.zrb.model.RootNode;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.TestRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Baut die sichtbare Scope-Sicht für eine TestAction:
 *
 * - variables: alle "normalen" Variablen, also BeforeAll + BeforeEach + Case.before
 * - templates: alle Lazy-Templates (mit *name in der UI)
 *
 * Shadowing-Regel:
 * Case überschreibt Suite überschreibt Root.
 *
 * Wichtig:
 *   Case.before      -> variables
 *   Suite.beforeEach -> variables
 *   Suite.beforeAll  -> variables
 *   Root.beforeEach  -> variables
 *   Root.beforeAll   -> variables
 *
 *   Case.templates   -> templates
 *   Suite.templates  -> templates
 *   Root.templates   -> templates
 */
public class GivenLookupService {

    public static class ScopeData {
        public final java.util.Map<String,String> variables = new LinkedHashMap<>();
        public final java.util.Map<String,String> templates = new LinkedHashMap<>();
    }

    private final TestRegistry repo = TestRegistry.getInstance();

    public ScopeData collectScopeForAction(TestAction action) {
        ScopeData out = new ScopeData();
        if (action == null) return out;

        TestCase tc   = repo.findCaseById(action.getParentId());
        TestSuite su  = (tc != null) ? repo.findSuiteById(tc.getParentId()) : null;
        RootNode root = repo.getRoot();

        // 1. Root
        if (root != null) {
            // Variablen: beforeAll + beforeEach
            mergeInto(out.variables, root.getBeforeAll(), root.getBeforeAllEnabled());
            mergeInto(out.variables, root.getBeforeEach(), root.getBeforeEachEnabled());
            // Templates:
            mergeInto(out.templates, root.getTemplates(), root.getTemplatesEnabled());
        }

        // 2. Suite
        if (su != null) {
            mergeInto(out.variables, su.getBeforeAll(), su.getBeforeAllEnabled());
            mergeInto(out.variables, su.getBeforeEach(), su.getBeforeEachEnabled());
            mergeInto(out.templates, su.getTemplates(), su.getTemplatesEnabled());
        }

        // 3. Case
        if (tc != null) {
            mergeInto(out.variables, tc.getBefore(), tc.getBeforeEnabled());     // Case.before verhält sich wie BeforeEach
            mergeInto(out.templates, tc.getTemplates(), tc.getTemplatesEnabled());  // Case.templates
        }

        return out;
    }

    /**
     * putAll mit Shadowing:
     * spätere Aufrufer überschreiben frühere Werte => genau was wir wollen.
     */
    private void mergeInto(Map<String,String> target,
                           Map<String,String> src,
                           Map<String, Boolean> enabled) {
        if (src == null) return;
        for (Map.Entry<String,String> e : src.entrySet()) {
            String key = e.getKey();
            if (key == null) continue;
            if (!isEnabled(enabled, key)) continue;
            target.put(key, e.getValue());
        }
    }

    private boolean isEnabled(Map<String, Boolean> enabled, String key) {
        if (enabled == null) return true;
        Boolean val = enabled.get(key);
        return val == null || val.booleanValue();
    }
}
