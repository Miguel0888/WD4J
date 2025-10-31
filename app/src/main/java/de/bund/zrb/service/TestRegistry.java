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
     *      "testSuites": [ ... ]
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
     */
    public void load() {
        SettingsService ss = SettingsService.getInstance();

        // 1) Neues Format probieren (DTO mit RootNode)
        {
            Type dtoType = new TypeToken<TestFileDTO>() {}.getType();
            TestFileDTO dto = ss.load("tests.json", dtoType);
            if (dto != null && dto.getRoot() != null) {
                this.root = dto.getRoot();

                ensureRootId(this.root);
                repairTreeIdsAndParents(this.root);
                return;
            }
        }

        // 2) Altes Format probieren (Liste von Suites)
        {
            Type legacyType = new TypeToken<List<TestSuite>>() {}.getType();
            List<TestSuite> loadedSuites = ss.load("tests.json", legacyType);
            if (loadedSuites != null) {
                RootNode newRoot = new RootNode(); // bekommt im Ctor eigentlich schon 'ne UUID
                // RootNode hat vermutlich eine Mutable-Liste testSuites. Wir hängen hier einfach an:
                if (newRoot.getTestSuites() == null) {
                    // NOTE: falls RootNode.testSuites null wäre (sollte eigentlich nie null sein),
                    // initialisieren wir es über Reflection.
                    forceInitListIfNull(newRoot, "testSuites");
                }
                newRoot.getTestSuites().addAll(loadedSuites);

                ensureRootId(newRoot);
                repairTreeIdsAndParents(newRoot);

                this.root = newRoot;
                return;
            }
        }

        // 3) nichts geladen -> leeren Default-Root behalten
        ensureRootId(this.root);
    }

    // -------------------------------------------------
    // interne Helfer
    // -------------------------------------------------

    /**
     * Stelle sicher, dass Root eine ID hat.
     * Falls Gson den No-Arg-Konstruktor aufgerufen hat und dabei id nicht gesetzt hat,
     * forcieren wir hier eine UUID.
     */
    private void ensureRootId(RootNode r) {
        if (r == null) return;
        if (isBlank(r.getId())) {
            // versuchen erst setter, dann Reflexion
            setFieldViaReflection(r, "id", UUID.randomUUID().toString());
        }
        // zusätzlich sicherstellen, dass die Suite-Liste nicht null ist
        if (r.getTestSuites() == null) {
            forceInitListIfNull(r, "testSuites");
        }
    }

    /**
     * Läuft den kompletten Baum runter und stellt Konsistenz her:
     *
     * - RootNode.id != null
     * - Jede Suite:
     *      - id != null
     *      - parentId = root.id
     *      - testCases != null
     * - Jeder TestCase:
     *      - id != null
     *      - parentId = suite.id
     *      - when != null
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
            // NOTE: falls RootNode.testSuites null war, initialisieren
            forceInitListIfNull(r, "testSuites");
            suites = r.getTestSuites();
        }

        for (TestSuite suite : suites) {
            if (suite == null) continue;

            // Suite-ID sicherstellen
            if (isBlank(suite.getId())) {
                setFieldViaReflection(suite, "id", UUID.randomUUID().toString());
            }

            // parentId Suite -> Root
            if (isBlank(suite.getParentId())) {
                suite.setParentId(r.getId());
            }

            // sicherstellen, dass ihre Child-Listen nicht null sind
            if (suite.getTestCases() == null) {
                // NOTE: es gibt keinen setTestCases(), also Liste notfalls per Reflexion initialisieren
                forceInitListIfNull(suite, "testCases");
            }
            if (suite.getGiven() == null) {
                forceInitListIfNull(suite, "given");
            }
            if (suite.getThen() == null) {
                forceInitListIfNull(suite, "then");
            }

            // Fälle durchgehen
            List<TestCase> cases = suite.getTestCases();
            for (TestCase tc : cases) {
                if (tc == null) continue;

                // Case-ID sicherstellen
                if (isBlank(tc.getId())) {
                    setFieldViaReflection(tc, "id", UUID.randomUUID().toString());
                }

                // parentId Case -> Suite
                if (isBlank(tc.getParentId())) {
                    tc.setParentId(suite.getId());
                }

                // Null-Listen im Case absichern
                if (tc.getWhen() == null) {
                    forceInitListIfNull(tc, "when");
                }
                if (tc.getGiven() == null) {
                    forceInitListIfNull(tc, "given");
                }
                if (tc.getThen() == null) {
                    forceInitListIfNull(tc, "then");
                }

                // Actions durchgehen
                List<TestAction> steps = tc.getWhen();
                for (TestAction a : steps) {
                    if (a == null) continue;

                    // Action-ID sicherstellen
                    if (isBlank(a.getId())) {
                        setFieldViaReflection(a, "id", UUID.randomUUID().toString());
                    }

                    // parentId Action -> Case
                    if (isBlank(a.getParentId())) {
                        a.setParentId(tc.getId());
                    }

                    // ActionType absichern (default WHEN)
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
     * Hilfsfunktion:
     *  - Falls eine List-Property null ist (z.B. weil aus altem JSON ohne dieses Feld kam),
     *    erzeugen wir eine neue ArrayList<>().
     *  - Wir machen das über Reflection, weil TestSuite/TestCase absichtlich
     *    keinen öffentlichen Setter für diese Collections haben.
     */
    @SuppressWarnings("unchecked")
    private void forceInitListIfNull(Object bean, String fieldName) {
        if (bean == null) return;
        try {
            java.lang.reflect.Field f = bean.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object current = f.get(bean);
            if (current == null) {
                // frische Liste reinschreiben
                f.set(bean, new ArrayList());
            }
        } catch (Exception ignore) {
            // Worst case: Liste bleibt null. Code oben prüft dann ggf. mit != null weiter.
        }
    }

    /**
     * Generische Helper-Methode zum Setzen einfacher Felder (id, etc.).
     * Versucht zuerst einen Setter (setId / setParentId ...).
     * Wenn's keinen Setter gibt, schreibt per Reflection ins Feld.
     */
    private void setFieldViaReflection(Object bean, String fieldName, String value) {
        if (bean == null || value == null) return;
        try {
            // 1. Setter probieren
            String setterName = "set" +
                    Character.toUpperCase(fieldName.charAt(0)) +
                    fieldName.substring(1);
            try {
                java.lang.reflect.Method m = bean.getClass().getMethod(setterName, String.class);
                m.invoke(bean, value);
                return;
            } catch (NoSuchMethodException ignore) {
                // Kein passender Setter -> direkter Fieldzugriff
            }

            // 2. Direkt ins Feld schreiben
            java.lang.reflect.Field f = bean.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(bean, value);
        } catch (Exception ignore) {
            // Wenn wir es gar nicht setzen können, leben wir halt damit.
        }
    }
}
