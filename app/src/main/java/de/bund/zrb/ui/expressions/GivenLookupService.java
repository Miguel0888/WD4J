package de.bund.zrb.ui.expressions;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.RootNode;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.TestRegistry;

import java.util.*;

/**
 * Liefert alle im Scope sichtbaren Namen für eine TestAction.
 *
 * Wir unterscheiden drei Kategorien:
 *  - beforeEach-Variablen   (werden ohne Präfix angezeigt)
 *  - beforeAll-Variablen    (werden mit Präfix "①" angezeigt)
 *  - templates              (werden mit Präfix "*" angezeigt)
 *
 * Shadowing-Regeln:
 * - Case überschreibt Suite überschreibt Root.
 *   (D.h. wenn Root "username" hat und Suite auch "username", gewinnt Suite.)
 *
 * Technische Annahmen am Modell:
 * - RootNode:
 *      getBeforeAll()    -> List<GivenCondition>
 *      getBeforeEach()   -> List<GivenCondition>
 *      getTemplates()    -> List<GivenCondition>
 * - TestSuite:
 *      getBeforeAll(), getBeforeEach(), getTemplates()
 * - TestCase:
 *      getBeforeEach(), getTemplates()
 *   (TestCase hat KEIN beforeAll und das ist beabsichtigt.)
 *
 * Jede GivenCondition speichert Name/Expression in gc.getValue()
 * als "name=<NAME>&expressionRaw=<EXPR>" (wie von dir etabliert).
 *
 * Für Arbeitspaket 1 interessiert uns nur der "name"-Teil.
 */
public class GivenLookupService {

    private final TestRegistry repo;

    public GivenLookupService(TestRegistry repo) {
        this.repo = repo;
    }

    /**
     * Hole die ScopeData für eine konkrete Action.
     * Falls irgendwas fehlt, liefern wir leere Sets zurück, kein Crash.
     */
    public ScopeData buildScopeDataForAction(TestAction action) {
        if (action == null) {
            return new ScopeData();
        }

        // 1. Chain auflösen: Action -> Case -> Suite -> Root
        TestCase theCase = repo.findCaseById(action.getParentId());
        TestSuite theSuite = null;
        if (theCase != null) {
            theSuite = repo.findSuiteById(theCase.getParentId());
        }
        RootNode root = repo.getRoot();

        // 2. Namen sammeln (Maps zum Shadowing nutzen)
        //    Wir bauen drei Maps: beforeEachVars, beforeAllVars, templates
        //
        //    Wir laufen VON ROOT NACH OBEN (Root -> Suite -> Case),
        //    damit weiter oben (Case) die Werte überschreibt.

        Map<String, GivenCondition> beforeEachVars = new LinkedHashMap<String, GivenCondition>();
        Map<String, GivenCondition> beforeAllVars  = new LinkedHashMap<String, GivenCondition>();
        Map<String, GivenCondition> templates      = new LinkedHashMap<String, GivenCondition>();

        // --- Root einbringen
        if (root != null) {
            mergeListIntoMap(root.getBeforeEach(), beforeEachVars);
            mergeListIntoMap(root.getBeforeAll(),  beforeAllVars);
            mergeListIntoMap(root.getTemplates(),  templates);
        }

        // --- Suite einbringen
        if (theSuite != null) {
            mergeListIntoMap(theSuite.getBeforeEach(), beforeEachVars);
            mergeListIntoMap(theSuite.getBeforeAll(),  beforeAllVars);
            mergeListIntoMap(theSuite.getTemplates(),  templates);
        }

        // --- Case einbringen
        if (theCase != null) {
            // Case hat KEIN beforeAll by design.
            mergeListIntoMap(safeList(theCase.getBeforeEach()), beforeEachVars);
            mergeListIntoMap(safeList(theCase.getTemplates()),  templates);
        }

        // 3. Extrahiere nur die Namen (Schlüssel "name=" in der Value-Map der GivenCondition)
        //    und schreibe sie in ScopeData
        ScopeData data = new ScopeData();

        // BeforeEachVars -> normale Variablen (ohne Präfix)
        for (Map.Entry<String, GivenCondition> e : beforeEachVars.entrySet()) {
            data.beforeEachNames.add(e.getKey()); // z.B. "username"
        }

        // BeforeAllVars -> Variablen, aber mit Präfix "①"
        for (Map.Entry<String, GivenCondition> e : beforeAllVars.entrySet()) {
            data.beforeAllNames.add(e.getKey()); // wir präfixen erst im UI
        }

        // Templates -> Funktionszeiger, UI bekommt später "*" davor
        for (Map.Entry<String, GivenCondition> e : templates.entrySet()) {
            data.templateNames.add(e.getKey()); // z.B. "otpCode"
        }

        return data;
    }

    /**
     * Hilfsfunktion: GivenCondition-Liste in die Map mergen.
     * - extrahiert den "name" aus gc.getValue()
     * - überschreibt vorhandene Keys (Shadowing).
     */
    private void mergeListIntoMap(List<GivenCondition> list,
                                  Map<String, GivenCondition> target) {
        if (list == null) return;
        for (GivenCondition gc : list) {
            if (gc == null) continue;
            String name = extractName(gc);
            if (name == null || name.trim().isEmpty()) continue;
            target.put(name.trim(), gc); // überschreibt absichtlich
        }
    }

    /**
     * Extrahiere "name=..." aus gc.getValue().
     */
    private String extractName(GivenCondition gc) {
        Map<String,String> map = parseValueMap(gc.getValue());
        return map.get("name");
    }

    private List<GivenCondition> safeList(List<GivenCondition> in) {
        return (in != null) ? in : new ArrayList<GivenCondition>();
    }

    /**
     * key=value&key=value Parser (wie in deiner SuiteScopeEditorTab.GivenTableModel).
     * Wir brauchen hier nur "name", also keep it simple.
     */
    private Map<String,String> parseValueMap(String raw) {
        Map<String,String> result = new LinkedHashMap<String,String>();
        if (raw == null || raw.trim().isEmpty()) {
            return result;
        }
        String[] pairs = raw.split("&");
        for (int i = 0; i < pairs.length; i++) {
            String[] kv = pairs[i].split("=", 2);
            if (kv.length == 2) {
                result.put(kv[0], kv[1]);
            }
        }
        return result;
    }

    /**
     * Das geben wir an die UI weiter.
     * NICHT die Expressions selbst, nur die Namen.
     *
     * Wir unterscheiden:
     *  - beforeEachNames  ("username", "belegnummer", ...)
     *  - beforeAllNames   ("globalSessionId", ...)
     *  - templateNames    ("otpCode", "wrapText", ...)
     */
    public static class ScopeData {
        public final LinkedHashSet<String> beforeEachNames = new LinkedHashSet<String>();
        public final LinkedHashSet<String> beforeAllNames  = new LinkedHashSet<String>();
        public final LinkedHashSet<String> templateNames   = new LinkedHashSet<String>();
    }
}
