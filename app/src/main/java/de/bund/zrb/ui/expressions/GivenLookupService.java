package de.bund.zrb.ui.expressions;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.RootNode;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.TestRegistry;

import java.util.*;

/**
 * Baut die "sichtbaren Namen" (Variablen / Templates) für eine bestimmte TestAction auf.
 *
 * Kategorien:
 *  - beforeEachNames  → normale Variablen (werden OHNE Präfix angezeigt)
 *  - beforeAllNames   → einmalig ausgewertete Variablen (werden mit Präfix "①" angezeigt)
 *  - templateNames    → Lazy-Templates/Funktionen (werden mit Präfix "*" angezeigt)
 *
 * Shadowing:
 *  - Case überschreibt Suite überschreibt Root
 *    (lokale Definition gewinnt gegenüber globaler)
 *
 * Scopes nach deiner Fachlogik:
 *
 * RootNode:
 *   getBeforeAll()
 *   getBeforeEach()
 *   getTemplates()
 *
 * TestSuite:
 *   getBeforeAll()
 *   getBeforeEach()
 *   getTemplates()
 *
 * TestCase:
 *   getGiven()          ← das sind konkrete Werte für DIESEN Case
 *   getTemplates()      ← lazy Templates auf Case-Ebene
 *   // KEIN beforeEach(), KEIN beforeAll() am Case
 *
 * Zuordnung:
 *   Case.getGiven()                → beforeEachNames (lokale Variablen)
 *   Case.getTemplates()            → templateNames
 *
 *   Suite.getBeforeEach()          → beforeEachNames
 *   Suite.getTemplates()           → templateNames
 *   Suite.getBeforeAll()           → beforeAllNames
 *
 *   Root.getBeforeEach()           → beforeEachNames
 *   Root.getTemplates()            → templateNames
 *   Root.getBeforeAll()            → beforeAllNames
 *
 * Hinweis:
 *   beforeAllNames werden im UI als "①<name>" angeboten,
 *   templateNames werden als "*<name>" angeboten.
 *   beforeEachNames kommen plain ("username").
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
     * Liefert die sichtbaren Namen für eine bestimmte Action.
     * Bricht niemals mit NullPointer ab, sondern gibt leere Sets zurück.
     */
    public ScopeData collectScopeForAction(TestAction action) {
        ScopeData out = new ScopeData();
        if (action == null) {
            return out;
        }

        // 1. Hierarchie bestimmen (Action -> Case -> Suite -> Root)
        TestCase tc   = repo.findCaseById(action.getParentId());
        TestSuite st  = (tc != null) ? repo.findSuiteById(tc.getParentId()) : null;
        RootNode root = repo.getRoot();

        // Wir füllen drei Maps für Shadowing.
        // Wir laufen von ROOT → SUITE → CASE,
        // damit CASE zuletzt schreibt und damit gewinnt.
        Map<String, GivenCondition> accBeforeEach = new LinkedHashMap<>();
        Map<String, GivenCondition> accBeforeAll  = new LinkedHashMap<>();
        Map<String, GivenCondition> accTemplates  = new LinkedHashMap<>();

        // -------- Root
        if (root != null) {
            mergeNamed(root.getBeforeEach(),   accBeforeEach);
            mergeNamed(root.getBeforeAll(),    accBeforeAll);
            mergeNamed(root.getTemplates(),    accTemplates);
        }

        // -------- Suite
        if (st != null) {
            mergeNamed(st.getBeforeEach(),     accBeforeEach);
            mergeNamed(st.getBeforeAll(),      accBeforeAll);
            mergeNamed(st.getTemplates(),      accTemplates);
        }

        // -------- Case
        if (tc != null) {
            // Case hat KEIN beforeAll/beforeEach explizit.
            // Aber: die Givens dieses Cases gelten als lokale Variablen.
            mergeNamed(tc.getGiven(),          accBeforeEach);

            // Case-spezifische Templates (falls vorhanden)
            mergeNamed(callGetTemplatesIfExists(tc), accTemplates);
        }

        // Jetzt packen wir nur die Namen in ScopeData:
        out.beforeEachNames.addAll(accBeforeEach.keySet());
        out.beforeAllNames.addAll(accBeforeAll.keySet());
        out.templateNames.addAll(accTemplates.keySet());

        return out;
    }

    /**
     * Extrahiert aus einer Liste von GivenCondition jeweils den "name" aus dem value-Feld
     * (Format "name=foo&expressionRaw=...") und schreibt ihn in targetMap.
     * Shadowing: spätere Einträge überschreiben frühere Einträge gleichen Namens.
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
     * Holt "name=..." aus gc.getValue()
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
     * Case.getTemplates() ist evtl. vorhanden (du hast das vorgesehen),
     * aber nicht garantiert von Anfang an. Wir rufen es defensiv via Reflection,
     * um deinen bestehenden Code nicht hart umzubauen.
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
            // Case hat (noch) keine Templates
        }
        return null;
    }

    /**
     * Das Struktur-Objekt für die UI.
     * Wir geben NUR Namen raus, keine kompletten GivenCondition-Objekte.
     *
     * - beforeEachNames: normale Variablen (kein Präfix)
     * - beforeAllNames:  einmalige Variablen (im UI "①name")
     * - templateNames:   Templates (im UI "*name")
     */
    public static class ScopeData {
        public final LinkedHashSet<String> beforeEachNames = new LinkedHashSet<>();
        public final LinkedHashSet<String> beforeAllNames  = new LinkedHashSet<>();
        public final LinkedHashSet<String> templateNames   = new LinkedHashSet<>();
    }
}
