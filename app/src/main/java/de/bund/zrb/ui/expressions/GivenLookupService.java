package de.bund.zrb.ui.expressions;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.RootNode;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.TestRegistry;

import java.util.*;

/**
 * Baut die "sichtbaren Namen" (Variablen / Templates / BeforeAll) für eine bestimmte TestAction.
 *
 * Kategorien:
 *  - beforeEachNames  → normale Variablen (werden OHNE Präfix angezeigt)
 *      - Root.beforeEach
 *      - Suite.beforeEach
 *      - Case.before     (Case-spezifischer Tab "Before")
 *
 *  - beforeAllNames    → einmalige Variablen (werden mit Präfix "①" angezeigt)
 *      - Root.beforeAll
 *      - Suite.beforeAll
 *    (Case hat kein beforeAll)
 *
 *  - templateNames     → Lazy-Templates/Funktionen (werden mit Präfix "*" angezeigt)
 *      - Root.templates
 *      - Suite.templates
 *      - Case.templates (Case-spezifischer Tab "Templates")
 *
 * Shadowing-Regel:
 *   Case überschreibt Suite überschreibt Root (lokaler Name gewinnt)
 *   => Wir laufen in der Reihenfolge ROOT → SUITE → CASE und überschreiben Keys.
 */
public class GivenLookupService {

    private final TestRegistry repo;

    public GivenLookupService() {
        this.repo = TestRegistry.getInstance();
    }

    public GivenLookupService(TestRegistry repo) {
        this.repo = repo;
    }

    /**
     * Liefert die sichtbaren Namen (Strings) für die ComboBox im ActionEditorTab.
     * Niemals nulls zurückgeben.
     */
    public ScopeData collectScopeForAction(TestAction action) {
        ScopeData out = new ScopeData();
        if (action == null) {
            return out;
        }

        // 1. Kette bestimmen
        TestCase tc   = repo.findCaseById(action.getParentId());
        TestSuite st  = (tc != null) ? repo.findSuiteById(tc.getParentId()) : null;
        RootNode root = repo.getRoot();

        // 2. Drei Maps für Shadowing
        //    - keys = sichtbarer Name ohne Präfix
        //    - value = GivenCondition (nur um zu erkennen "existiert")
        // Wir überschreiben später einfach denselben Key, wenn tieferer Scope denselben Namen hat.
        Map<String, GivenCondition> accBeforeEach = new LinkedHashMap<>();
        Map<String, GivenCondition> accBeforeAll  = new LinkedHashMap<>();
        Map<String, GivenCondition> accTemplates  = new LinkedHashMap<>();

        // Reihenfolge: ROOT → SUITE → CASE (CASE gewinnt)

        // ----- ROOT -----
        if (root != null) {
            mergeNamed(root.getBeforeEach(),  accBeforeEach);
            mergeNamed(root.getBeforeAll(),   accBeforeAll);
            mergeNamed(root.getTemplates(),   accTemplates);
        }

        // ----- SUITE -----
        if (st != null) {
            mergeNamed(st.getBeforeEach(),    accBeforeEach);
            mergeNamed(st.getBeforeAll(),     accBeforeAll);
            mergeNamed(st.getTemplates(),     accTemplates);
        }

        // ----- CASE -----
        if (tc != null) {
            // Case hat KEIN beforeAll.
            // Case-"Before" → normale Variablen (beforeEachNames)
            mergeNamed(callGetBeforeIfExists(tc), accBeforeEach);

            // Case-"Templates" → templateNames
            mergeNamed(callGetTemplatesIfExists(tc), accTemplates);
        }

        // 3. Jetzt die Sets für die UI befüllen
        out.beforeEachNames.addAll(accBeforeEach.keySet());
        out.beforeAllNames.addAll(accBeforeAll.keySet());
        out.templateNames.addAll(accTemplates.keySet());

        return out;
    }

    /**
     * Schreibt alle Namen aus der Liste (GivenCondition.value enthält name=...)
     * in targetMap. Gleiche Keys werden überschrieben (Shadowing).
     */
    private void mergeNamed(List<GivenCondition> list,
                            Map<String, GivenCondition> targetMap) {
        if (list == null) return;
        for (GivenCondition gc : list) {
            if (gc == null) continue;
            String name = extractName(gc);
            if (name == null || name.trim().isEmpty()) continue;
            targetMap.put(name.trim(), gc);
        }
    }

    /**
     * Holt "name=..." aus gc.getValue().
     * Deine GivenCondition speichert z.B. "name=username&expressionRaw={{...}}"
     */
    private String extractName(GivenCondition gc) {
        Map<String,String> m = parseValueMap(gc.getValue());
        return m.get("name");
    }

    private Map<String,String> parseValueMap(String raw) {
        Map<String,String> result = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        String[] pairs = raw.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                result.put(kv[0], kv[1]);
            }
        }
        return result;
    }

    /**
     * Case.getBefore() per Reflection holen (damit wir deine bestehenden Modelle
     * so wenig wie möglich anfassen müssen).
     *
     * Erwartet: List<GivenCondition> getBefore()
     *
     * Falls es das (noch) nicht gibt, oder es wirft Exception:
     * -> null zurück
     */
    @SuppressWarnings("unchecked")
    private List<GivenCondition> callGetBeforeIfExists(TestCase tc) {
        if (tc == null) return null;
        try {
            java.lang.reflect.Method m = tc.getClass().getMethod("getBefore");
            Object v = m.invoke(tc);
            if (v instanceof List<?>) {
                return (List<GivenCondition>) v;
            }
        } catch (Exception ignore) {
            // Case hat (noch) kein "Before"
        }
        return null;
    }

    /**
     * Case.getTemplates() per Reflection holen.
     *
     * Erwartet: List<GivenCondition> getTemplates()
     *
     * Falls es das (noch) nicht gibt -> null zurück.
     */
    @SuppressWarnings("unchecked")
    private List<GivenCondition> callGetTemplatesIfExists(TestCase tc) {
        if (tc == null) return null;
        try {
            java.lang.reflect.Method m = tc.getClass().getMethod("getTemplates");
            Object v = m.invoke(tc);
            if (v instanceof List<?>) {
                return (List<GivenCondition>) v;
            }
        } catch (Exception ignore) {
            // Case hat (noch) kein "Templates"
        }
        return null;
    }

    /**
     * Rückgabeobjekt für die ComboBox.
     *
     * - beforeEachNames → normale Variablen (kein Präfix)
     * - beforeAllNames  → einmalige Variablen (im UI später "①name")
     * - templateNames   → Templates (im UI später "*name")
     */
    public static class ScopeData {
        public final LinkedHashSet<String> beforeEachNames = new LinkedHashSet<>();
        public final LinkedHashSet<String> beforeAllNames  = new LinkedHashSet<>();
        public final LinkedHashSet<String> templateNames   = new LinkedHashSet<>();
    }
}
