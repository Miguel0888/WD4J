package de.bund.zrb.model;

import java.util.UUID;

/**
 * Ein "Template"/Funktionszeiger.
 * Wird NICHT sofort evaluiert, sondern erst lazy
 * wenn im When mit *name referenziert.
 */
public class ScopeTemplateEntry {
    private String id;
    private String name;           // Bezeichner des Templates
    private String description;    // freier Text
    private String expressionRaw;  // z.B. {{otp({{username}})}}

    public ScopeTemplateEntry() {
        this.id = UUID.randomUUID().toString();
    }

    public ScopeTemplateEntry(String name, String expr) {
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
