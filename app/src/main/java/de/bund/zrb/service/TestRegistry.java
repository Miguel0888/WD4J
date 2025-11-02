package de.bund.zrb.service;

import com.google.gson.reflect.TypeToken;
import de.bund.zrb.model.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Zentrale "TestRepo".
 *
 * - Hält genau EIN RootNode mit UUID.
 * - Bietet weiterhin getAll() für Legacy-Code (liefert root.getTestSuites()).
 * - Kann altes JSON-Format (List<TestSuite>) laden und in RootNode "einbetten".
 * - Speichert ab jetzt im neuen Format { "root": { ... } }.
 */
public class TestRegistry {

    @Deprecated
    private boolean loadedFromLegacy = false;

    private static final TestRegistry INSTANCE = new TestRegistry();

    // unsere neue Wahrheit
    private RootNode root = new RootNode();

    private TestRegistry() {
        load();
    }

    public static TestRegistry getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------
    // Public API
    // -------------------------------------------------

    @Deprecated
    public boolean wasLoadedFromLegacy() {
        return loadedFromLegacy;
    }

    /** Neues API: komplette Root-Struktur holen. */
    public RootNode getRoot() {
        return root;
    }

    /** Legacy API: Liste aller Suites (Root-Kinder). */
    public List<TestSuite> getAll() {
        return root.getTestSuites();
    }

    /** Legacy API: Suite an Root anhängen. */
    public void addSuite(TestSuite suite) {
        if (suite == null) return;
        ensureRootId(root);

        // parentId pflegen
        suite.setParentId(root.getId());

        root.getTestSuites().add(suite);
    }

    /**
     * Persistiere die Daten im NEUEN Format:
     *
     * {
     *   "root": {
     *      "id": "...",
     *      "testSuites": [ ... ],
     *      "beforeAllVars": [...],
     *      "beforeEachVars": [...],
     *      "templates": [...]
     *   }
     * }
     */
    public void save() {
        TestFileDTO dto = new TestFileDTO();
        dto.setRoot(root);

        SettingsService.getInstance().save("tests.json", dto);
    }

    /**
     * Laden:
     *
     * 1. Versuch: Neues Format (TestFileDTO mit RootNode).
     * 2. Fallback: Altes Format (List<TestSuite>).
     *
     * Danach:
     * - UUIDs vergeben falls fehlend
     * - parentId-Kette reparieren
     * - fehlende Collections initialisieren (Root.beforeAllVars etc.)
     */
    public void load() {
        SettingsService ss = SettingsService.getInstance();

        // === 1) Neues Format versuchen ==========================
        try {
            Type dtoType = new TypeToken<TestFileDTO>() {}.getType();
            TestFileDTO dto = ss.load("tests.json", dtoType);
            if (dto != null && dto.getRoot() != null) {
                this.root = dto.getRoot();

                ensureRootId(this.root);
                repairTreeIdsAndParents(this.root);
                return;
            }
        } catch (Exception ignore) {
            // Datei evtl. noch im alten Format
        }

        // === 2) Altes Format versuchen (List<TestSuite>) ========
        try {
            Type legacyType = new TypeToken<List<TestSuite>>() {}.getType();
            List<TestSuite> loadedSuites = ss.load("tests.json", legacyType);
            if (loadedSuites != null) {
                RootNode newRoot = new RootNode();

                // safety init für testSuites
                if (newRoot.getTestSuites() == null) {
                    forceInitListIfNull(newRoot, "testSuites");
                }
                newRoot.getTestSuites().addAll(loadedSuites);

                ensureRootId(newRoot);
                repairTreeIdsAndParents(newRoot);

                this.root = newRoot;
                loadedFromLegacy = true;
                return;
            }
        } catch (Exception ignore) {
            // Datei vielleicht leer/kaputt
        }

        // === 3) Kein Glück: leeren Root anlegen =================
        ensureRootId(this.root);
        repairTreeIdsAndParents(this.root);
    }

    // -------------------------------------------------
    // Hilfsfunktionen für ID/Parent-Konsistenz
    // -------------------------------------------------

    /**
     * Stelle sicher, dass Root eine ID hat
     * und dass seine Collections nicht null sind.
     */
    private void ensureRootId(RootNode r) {
        if (r == null) return;

        if (r.getId() == null || r.getId().trim().isEmpty()) {
            setFieldViaReflection(r, "id", java.util.UUID.randomUUID().toString());
        }

        // testSuites-Liste absichern
        if (r.getTestSuites() == null) {
            forceInitListIfNull(r, "testSuites");
        }

        // Die drei Scope-Listen sind bei dir final + new ArrayList<>(),
        // also eigentlich nie null. Falls Gson sie aber per Reflection
        // überschrieben hat und null gesetzt hat, fangen wir es trotzdem ab:
        forceInitListIfNull(r, "beforeAllVars");
        forceInitListIfNull(r, "beforeEachVars");
        forceInitListIfNull(r, "templates");
    }

    /**
     * Läuft den kompletten Baum runter und stellt Konsistenz her:
     *
     * - RootNode.id != null
     * - Root hat beforeAllVars/beforeEachVars/templates-Listen != null
     * - Jede Suite:
     *      - id != null
     *      - parentId = root.id
     *      - testCases != null
     *      - given / then != null (legacy Felder)
     *      - beforeAll / beforeEach / templates != null (neue Felder)
     * - Jeder TestCase:
     *      - id != null
     *      - parentId = suite.id
     *      - when / given / then != null
     *      - beforeCase / templates != null (neue Felder auf Case-Ebene)
     * - Jede TestAction:
     *      - id != null
     *      - parentId = case.id
     *      - type != null (default WHEN)
     */
    private void repairTreeIdsAndParents(RootNode r) {
        if (r == null) return;

        ensureRootId(r);

        List<TestSuite> suites = r.getTestSuites();
        if (suites == null) {
            forceInitListIfNull(r, "testSuites");
            suites = r.getTestSuites();
        }

        for (TestSuite suite : suites) {
            if (suite == null) continue;

            // Suite-ID
            if (isBlank(suite.getId())) {
                setFieldViaReflection(suite, "id", UUID.randomUUID().toString());
            }

            // parentId Suite -> Root
            if (isBlank(suite.getParentId())) {
                suite.setParentId(r.getId());
            }

            // Listen der Suite absichern
            forceInitListIfNull(suite, "testCases");
            forceInitListIfNull(suite, "given");
            forceInitListIfNull(suite, "then");

            // NEU: Scopes der Suite
            forceInitListIfNull(suite, "beforeAll");
            forceInitListIfNull(suite, "beforeEach");
            forceInitListIfNull(suite, "templates");

            List<TestCase> cases = suite.getTestCases();
            for (TestCase tc : cases) {
                if (tc == null) continue;

                // Case-ID
                if (isBlank(tc.getId())) {
                    setFieldViaReflection(tc, "id", UUID.randomUUID().toString());
                }

                // parentId Case -> Suite
                if (isBlank(tc.getParentId())) {
                    tc.setParentId(suite.getId());
                }

                // "alte" Case-Listen absichern
                forceInitListIfNull(tc, "when");
                forceInitListIfNull(tc, "given");
                forceInitListIfNull(tc, "then");

                // NEU: Case-Scope-Listen absichern
                forceInitListIfNull(tc, "beforeCase");
                forceInitListIfNull(tc, "templates");

                List<TestAction> steps = tc.getWhen();
                for (TestAction a : steps) {
                    if (a == null) continue;

                    // Action-ID
                    if (isBlank(a.getId())) {
                        setFieldViaReflection(a, "id", UUID.randomUUID().toString());
                    }

                    // parentId Action -> Case
                    if (isBlank(a.getParentId())) {
                        a.setParentId(tc.getId());
                    }

                    // ActionType absichern
                    if (a.getType() == null) {
                        a.setType(TestAction.ActionType.WHEN);
                    }
                }
            }
        }
    }

    private boolean isBlank(String s) {
        return (s == null || s.trim().isEmpty());
    }

    /**
     * Falls eine List-Property null ist (z.B. alter JSON-Stand),
     * initialisieren wir sie per Reflection mit einer neuen ArrayList().
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void forceInitListIfNull(Object bean, String fieldName) {
        if (bean == null) return;
        try {
            java.lang.reflect.Field f = bean.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object current = f.get(bean);
            if (current == null) {
                f.set(bean, new ArrayList());
            }
        } catch (Exception ignore) {
            // worst case: bleibt halt null
        }
    }

    /**
     * Generische Helper-Methode zum Setzen einfacher Felder (id, parentId, ...).
     * Versucht zuerst einen Setter, dann direkten Fieldzugriff.
     */
    private void setFieldViaReflection(Object bean, String fieldName, String value) {
        if (bean == null || value == null) return;
        try {
            // 1. Setter probieren
            String setterName =
                    "set" +
                            Character.toUpperCase(fieldName.charAt(0)) +
                            fieldName.substring(1);
            try {
                java.lang.reflect.Method m = bean.getClass().getMethod(setterName, String.class);
                m.invoke(bean, value);
                return;
            } catch (NoSuchMethodException ignore) {
                // kein Setter, dann direkt ins Feld
            }

            java.lang.reflect.Field f = bean.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(bean, value);
        } catch (Exception ignore) {
            // wenn wir's gar nicht setzen können, leben wir damit
        }
    }

    // -------------------------------------------------
    // Lookup-Helfer (werden wir gleich für GivenLookupService brauchen)
    // -------------------------------------------------

    /**
     * Suche eine Suite anhand ihrer ID.
     * Returns null, wenn keine gefunden.
     */
    public TestSuite findSuiteById(String suiteId) {
        if (isBlank(suiteId)) return null;
        if (root == null || root.getTestSuites() == null) return null;

        for (TestSuite s : root.getTestSuites()) {
            if (s != null && suiteId.equals(s.getId())) {
                return s;
            }
        }
        return null;
    }

    /**
     * Suche einen Case anhand seiner ID.
     * Returns null, wenn nicht gefunden.
     */
    public TestCase findCaseById(String caseId) {
        if (isBlank(caseId)) return null;
        if (root == null || root.getTestSuites() == null) return null;

        for (TestSuite s : root.getTestSuites()) {
            if (s == null || s.getTestCases() == null) continue;
            for (TestCase tc : s.getTestCases()) {
                if (tc != null && caseId.equals(tc.getId())) {
                    return tc;
                }
            }
        }
        return null;
    }
}
