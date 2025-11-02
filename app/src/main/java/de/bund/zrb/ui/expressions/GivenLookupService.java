package de.bund.zrb.ui.expressions;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.RootNode;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.TestRegistry;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Baut die Liste möglicher Scope-Referenzen (Variablen / Templates),
 * die in der Value-Combobox einer Action angezeigt werden.
 *
 * Ergebnis:
 *  - variables: normale Variablen (werden beim Lauf vorher evaluiert)
 *  - templates: Funktionszeiger (werden lazy zur Laufzeit ausgewertet), mit *-Prefix in der UI
 *
 * Wichtig:
 * - beforeAll NICHT zur Auswahl anbieten,
 *   weil beforeAll-Variablen einmalig am Teststart evaluiert werden
 *   und nicht für jede Action neu.
 *
 * - Case hat ggf. keine beforeEach -> also nicht aufrufen wenn's das nicht gibt.
 */
public class GivenLookupService {

    public static class ScopeData {
        public final LinkedHashSet<String> variables = new LinkedHashSet<String>();
        public final LinkedHashSet<String> templates = new LinkedHashSet<String>();
    }

    public ScopeData collectScopeForAction(TestAction action) {
        ScopeData out = new ScopeData();
        if (action == null) return out;

        TestRegistry repo = TestRegistry.getInstance();

        // Parent Case
        TestCase tc = repo.findCaseById(action.getParentId());
        if (tc != null) {
            collectFromCase(tc, out);

            // Suite
            TestSuite suite = repo.findSuiteById(tc.getParentId());
            if (suite != null) {
                collectFromSuite(suite, out);
            }
        }

        // Root IMMER am Ende
        RootNode root = repo.getRoot();
        if (root != null) {
            collectFromRoot(root, out);
        }

        return out;
    }

    private void collectFromCase(TestCase tc, ScopeData out) {
        if (tc == null) return;

        // Case-level "Given" -> behandeln wir als Variablen-Quelle
        addVariablesFromList(out, tc.getGiven());

        // Case-level templates (falls vorhanden)
        if (hasMethodTemplates(tc)) {
            addTemplatesFromList(out, getTemplates(tc));
        }

        // KEIN beforeEach hier aufrufen, weil es das auf Case-Ebene bei dir nicht gibt.
    }

    private void collectFromSuite(TestSuite suite, ScopeData out) {
        if (suite == null) return;

        // Suite.beforeEach -> Variablen
        addVariablesFromList(out, suite.getBeforeEach());

        // Suite.templates -> Templates
        addTemplatesFromList(out, suite.getTemplates());

        // Suite.beforeAll NICHT anbieten -> absichtlich weggelassen
    }

    private void collectFromRoot(RootNode root, ScopeData out) {
        if (root == null) return;

        // Root.beforeEach -> Variablen
        addVariablesFromList(out, root.getBeforeEach());

        // Root.templates -> Templates
        addTemplatesFromList(out, root.getTemplates());

        // Root.beforeAll NICHT anbieten
    }

    private void addVariablesFromList(ScopeData out, List<GivenCondition> list) {
        if (list == null) return;
        for (GivenCondition gc : list) {
            String n = extractName(gc);
            if (notBlank(n)) {
                out.variables.add(n);
            }
        }
    }

    private void addTemplatesFromList(ScopeData out, List<GivenCondition> list) {
        if (list == null) return;
        for (GivenCondition gc : list) {
            String n = extractName(gc);
            if (notBlank(n)) {
                out.templates.add(n);
            }
        }
    }

    /**
     * Holt den "name" aus dem GivenCondition.value String:
     *   name=username&expressionRaw={{user.name}}
     */
    private String extractName(GivenCondition gc) {
        if (gc == null) return null;
        String raw = gc.getValue();
        if (raw == null) return null;

        String[] pairs = raw.split("&");
        for (String p : pairs) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2 && "name".equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    // --- Kleine Reflection-Helfer, falls TestCase.getTemplates() (etc.) noch nicht existiert ---

    private boolean hasMethodTemplates(Object bean) {
        try {
            bean.getClass().getMethod("getTemplates");
            return true;
        } catch (NoSuchMethodException ex) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private List<GivenCondition> getTemplates(Object bean) {
        try {
            java.lang.reflect.Method m = bean.getClass().getMethod("getTemplates");
            Object v = m.invoke(bean);
            if (v instanceof List<?>) {
                return (List<GivenCondition>) v;
            }
        } catch (Exception ignore) {
        }
        return null;
    }
}
