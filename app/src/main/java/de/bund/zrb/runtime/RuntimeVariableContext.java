package de.bund.zrb.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RuntimeVariableContext = Symboltabelle für den laufenden Testrun.
 *
 * Hält 3 Ebenen:
 *   - rootVars / rootTemplates: gelten für den ganzen Lauf
 *   - suiteVars / suiteTemplates: gelten innerhalb der aktuellen Suite
 *   - caseVars / caseTemplates: gelten innerhalb des aktuellen Cases
 *
 * Shadowing-Regel:
 *   Case überschreibt Suite überschreibt Root.
 *
 * Lebenszyklus:
 *   - Beim Start einer neuen Suite: enterSuite()
 *       -> löscht suiteVars/suiteTemplates
 *       -> löscht caseVars/caseTemplates (neue Suite = neuer Case-Kontext)
 *
 *   - Beim Start eines neuen Case: enterCase()
 *       -> löscht caseVars/caseTemplates
 *
 * Befüllen:
 *   - Wenn BeforeAll/BeforeEach/Before ausgewertet ist und
 *     "username" -> "alice42" rauskommt,
 *     dann runtimeCtx.setCaseVar("username", "alice42") o.ä.
 *
 * ValueScope:
 *   - buildCaseScope() gibt ein Snapshot-Objekt zurück,
 *     das für eine Action gilt.
 *
 * Zusätzlich: reference auf ExpressionRegistry (für Function Calls).
 */
public class RuntimeVariableContext {

    private final ExpressionRegistry exprRegistry;

    // --- Root-Ebene (bleibt über gesamten Run erhalten) ---
    private final Map<String,String> rootVars        = new LinkedHashMap<>();
    private final Map<String,String> rootTemplates   = new LinkedHashMap<>();

    // --- Aktuelle Suite-Ebene ---
    private final Map<String,String> suiteVars       = new LinkedHashMap<>();
    private final Map<String,String> suiteTemplates  = new LinkedHashMap<>();

    // --- Aktuelle Case-Ebene ---
    private final Map<String,String> caseVars        = new LinkedHashMap<>();
    private final Map<String,String> caseTemplates   = new LinkedHashMap<>();

    public RuntimeVariableContext(ExpressionRegistry exprRegistry) {
        this.exprRegistry = exprRegistry;
    }

    // -------------------------------------------------------
    // Lifecycle Hooks
    // -------------------------------------------------------

    /** Neue Suite beginnt -> Suite-spezifische Maps leeren, Case auch leeren. */
    public void enterSuite() {
        suiteVars.clear();
        suiteTemplates.clear();
        enterCase(); // ein frischer Suite-Start impliziert auch ein frischer Case-Kontext
    }

    /** Neuer Case beginnt -> Case-spezifische Maps leeren. */
    public void enterCase() {
        caseVars.clear();
        caseTemplates.clear();
    }

    // -------------------------------------------------------
    // Setters fürs Befüllen der Symboltabellen
    // -------------------------------------------------------

    public void setRootVar(String key, String value) {
        if (key != null) rootVars.put(key, value != null ? value : "");
    }

    public void setSuiteVar(String key, String value) {
        if (key != null) suiteVars.put(key, value != null ? value : "");
    }

    public void setCaseVar(String key, String value) {
        if (key != null) caseVars.put(key, value != null ? value : "");
    }

    public void setRootTemplate(String key, String templateExpr) {
        if (key != null) rootTemplates.put(key, templateExpr != null ? templateExpr : "");
    }

    public void setSuiteTemplate(String key, String templateExpr) {
        if (key != null) suiteTemplates.put(key, templateExpr != null ? templateExpr : "");
    }

    public void setCaseTemplate(String key, String templateExpr) {
        if (key != null) caseTemplates.put(key, templateExpr != null ? templateExpr : "");
    }

    /**
     * Bulk-Fill Helfer: wenn du schon Maps<String,String> hast.
     * Beispiel: du hast suite.getBeforeEach() ausgewertet zu Map<name,wert>
     * -> call fillSuiteVarsFromMap(...)
     */
    public void fillRootVarsFromMap(Map<String,String> m) {
        if (m == null) return;
        for (Map.Entry<String,String> e : m.entrySet()) {
            setRootVar(e.getKey(), e.getValue());
        }
    }

    public void fillSuiteVarsFromMap(Map<String,String> m) {
        if (m == null) return;
        for (Map.Entry<String,String> e : m.entrySet()) {
            setSuiteVar(e.getKey(), e.getValue());
        }
    }

    public void fillCaseVarsFromMap(Map<String,String> m) {
        if (m == null) return;
        for (Map.Entry<String,String> e : m.entrySet()) {
            setCaseVar(e.getKey(), e.getValue());
        }
    }

    public void fillRootTemplatesFromMap(Map<String,String> m) {
        if (m == null) return;
        for (Map.Entry<String,String> e : m.entrySet()) {
            setRootTemplate(e.getKey(), e.getValue());
        }
    }

    public void fillSuiteTemplatesFromMap(Map<String,String> m) {
        if (m == null) return;
        for (Map.Entry<String,String> e : m.entrySet()) {
            setSuiteTemplate(e.getKey(), e.getValue());
        }
    }

    public void fillCaseTemplatesFromMap(Map<String,String> m) {
        if (m == null) return;
        for (Map.Entry<String,String> e : m.entrySet()) {
            setCaseTemplate(e.getKey(), e.getValue());
        }
    }

    // -------------------------------------------------------
    // Zugriff, den TestPlayerService braucht
    // -------------------------------------------------------

    /**
     * Build the "effective" scope for a WHEN-Step in the aktuellen Case.
     * Shadowing-Regel:
     *   - caseVars zuerst, dann suiteVars, dann rootVars
     *   - caseTemplates zuerst, dann suiteTemplates, dann rootTemplates
     *
     * Außerdem geben wir exprRegistry mit, weil Templates später Funktionen
     * aufrufen müssen.
     */
    public ValueScope buildCaseScope() {
        return new ValueScope(
                rootVars, suiteVars, caseVars,
                rootTemplates, suiteTemplates, caseTemplates,
                exprRegistry
        );
    }

    public ValueScope buildCaseScopeForActionOnly() {
        // Build a ValueScope that only contains case-level vars/templates; root/suite maps empty
        return new ValueScope(
                new java.util.LinkedHashMap<String,String>(), // empty rootVars
                new java.util.LinkedHashMap<String,String>(), // empty suiteVars
                caseVars,
                new java.util.LinkedHashMap<String,String>(), // empty rootTemplates
                new java.util.LinkedHashMap<String,String>(), // empty suiteTemplates
                caseTemplates,
                exprRegistry
        );
    }
}
