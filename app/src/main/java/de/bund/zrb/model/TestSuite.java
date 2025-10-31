package de.bund.zrb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    // ---------------------------------------------------------------------------
    // Identity / hierarchy
    // ---------------------------------------------------------------------------

    /** Unique id of this suite. */
    private String id;

    /** Points to owning RootNode's id. */
    private String parentId;

    // ---------------------------------------------------------------------------
    // Metadata
    // ---------------------------------------------------------------------------

    private String name;
    private String description; // optional Freitext-Beschreibung

    // ---------------------------------------------------------------------------
    // Content
    // ---------------------------------------------------------------------------

    /** Suite-level Givens (BeforeEach-ähnlich auf Suite-Ebene aktuell). */
    private final List<GivenCondition> given = new ArrayList<GivenCondition>();

    /** Suite-level Thens (Erwartungen auf Suite-Ebene, existiert schon bei dir). */
    private final List<ThenExpectation> then = new ArrayList<ThenExpectation>();

    /** Child test cases of this suite. */
    private final List<TestCase> testCases = new ArrayList<TestCase>();

    // ---------------------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------------------

    /**
     * No-arg constructor for Gson / deserialization.
     * Also used when we clone/duplicate Suites programmatically.
     * Ensures id is never null for new instances created via `new TestSuite()`.
     *
     * Achtung:
     * - Falls altes JSON geladen wird und KEINE id drin steht,
     *   überschreiben wir später in TestRegistry.repairTreeIdsAndParents(...)
     *   nochmal gezielt, aber hier geben wir schon mal eine frische UUID mit.
     */
    public TestSuite() {
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Convenience ctor used e.g. when creating "Neue Testsuite" aus dem Kontextmenü.
     * Gives the suite a new UUID and sets its display name.
     */
    public TestSuite(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    /**
     * Legacy-style convenience ctor (du hattest den schon),
     * plus direkte Übernahme einer bestehenden Case-Liste.
     */
    public TestSuite(String name, List<TestCase> testCases) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        if (testCases != null) {
            this.testCases.addAll(testCases);
        }
    }

    // ---------------------------------------------------------------------------
    // Getters / Setters
    // ---------------------------------------------------------------------------

    // --- identity ---
    public String getId() {
        return id;
    }

    /**
     * Used by TestRegistry.repairTreeIdsAndParents() to force a deterministic id
     * for suites that came from legacy JSON (id was missing).
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * The id of the parent RootNode.
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * Called by TestRegistry.repairTreeIdsAndParents() when wiring the model.
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    // --- metadata ---
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Freeform description of the suite, shown in UI etc.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // --- content collections ---
    /**
     * Suite-level GivenConditions (Before/Setup auf Suite-Ebene).
     * Direct list reference is returned intentionally (like before),
     * so UI / services can add/remove entries in-place.
     */
    public List<GivenCondition> getGiven() {
        return given;
    }

    /**
     * Suite-level Then expectations.
     */
    public List<ThenExpectation> getThen() {
        return then;
    }

    /**
     * Child TestCases.
     */
    public List<TestCase> getTestCases() {
        return testCases;
    }

    // ---------------------------------------------------------------------------
    // equals / hashCode
    // ---------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestSuite)) return false;

        TestSuite that = (TestSuite) o;

        // Primär nach id vergleichen, wenn vorhanden.
        if (id != null && that.id != null) {
            return id.equals(that.id);
        }

        // Fallback für sehr alte Objekte ohne id:
        // Gleichheit über Namen ist schwach, aber besser als nichts.
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        // Stabiler Hash über id, wenn vorhanden
        if (id != null) {
            return id.hashCode();
        }
        // Fallback auf name für Legacy-Objekte
        return (name != null ? name.hashCode() : 0);
    }
}
