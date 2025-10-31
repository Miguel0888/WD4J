package de.bund.zrb.model;

import java.util.UUID;

/**
 * Eine "Variable" im Scope (Before/BeforeAll/etc.).
 * Wird sofort beim Eintritt in den Scope evaluiert.
 */
public class ScopeVariableEntry {
    private String id;
    private String name;           // Bezeichner, z.B. "username" oder "otp"
    private String description;    // freier Text vom Nutzer
    private String expressionRaw;  // Mustache/Template-Ausdruck

    public ScopeVariableEntry() {
        this.id = UUID.randomUUID().toString();
    }

    public ScopeVariableEntry(String name, String expr) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.expressionRaw = expr;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; } // für Gson/Migration

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getExpressionRaw() { return expressionRaw; }
    public void setExpressionRaw(String expressionRaw) { this.expressionRaw = expressionRaw; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
