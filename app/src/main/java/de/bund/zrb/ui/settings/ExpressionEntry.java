package de.bund.zrb.ui.settings;

import de.bund.zrb.expressions.domain.FunctionMetadata;
import java.util.Collections;
import java.util.List;

/** Persistence model: source code + metadata in expressions.json */
public final class ExpressionEntry {

    private String code;
    private FunctionMetadata meta;

    public ExpressionEntry() { /* for Gson */ }

    public ExpressionEntry(String code, FunctionMetadata meta) {
        this.code = code != null ? code : "";
        this.meta = normalize(meta);
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code != null ? code : ""; }

    public FunctionMetadata getMeta() { return meta; }
    public void setMeta(FunctionMetadata meta) { this.meta = normalize(meta); }

    private FunctionMetadata normalize(FunctionMetadata m) {
        if (m == null) {
            return new FunctionMetadata("", "", Collections.<String>emptyList(), Collections.<String>emptyList());
        }
        // Stelle sicher: Listen nie null
        List<String> names = m.getParameterNames() != null ? m.getParameterNames() : Collections.<String>emptyList();
        List<String> descs = m.getParameterDescriptions() != null ? m.getParameterDescriptions() : Collections.<String>emptyList();
        return new FunctionMetadata(
                m.getName() != null ? m.getName() : "",
                m.getDescription() != null ? m.getDescription() : "",
                names,
                descs
        );
    }
}
