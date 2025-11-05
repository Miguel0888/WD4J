// de/bund/zrb/runtime/ExpressionRegistryImpl.java
package de.bund.zrb.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.bund.zrb.compiler.InMemoryJavaCompiler;
import de.bund.zrb.ui.settings.ExpressionExamples;

import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionContext;
import de.bund.zrb.expressions.domain.FunctionMetadata;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

/**
 * Vereinigt Benutzer-Expressions (kompiliert zur Laufzeit) und eingebaute Funktionen (Builtins, kein Compile).
 * - Builtins werden über BuiltinFunctionCatalog bereitgestellt.
 * - evaluate(..) ruft zuerst Builtins direkt auf, danach ggf. die User-Implementierung via Compiler.
 * - Builtins tauchen in getKeys() auf, sind nicht löschbar und liefern bei getCode() kein Optional mit Source.
 */
public class ExpressionRegistryImpl implements ExpressionRegistry {

    private static final String FILE_NAME = "expressions.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, ExpressionEntry>>() {}.getType();

    private static ExpressionRegistryImpl instance;

    // Benutzerdefinierte Expressions (Name -> Code + Metadaten)
    private final Map<String, ExpressionEntry> expressions = new LinkedHashMap<String, ExpressionEntry>();
    private final File file;
    private final InMemoryJavaCompiler compiler = new InMemoryJavaCompiler();

    // Externer Katalog der Builtins (bereitgestellt von dir)
    private final BuiltinFunctionCatalog builtins = new BuiltinFunctionCatalog();

    private ExpressionRegistryImpl() {
        this.file = new File(getSettingsFolder(), FILE_NAME);
        reload();
    }

    public static synchronized ExpressionRegistryImpl getInstance() {
        if (instance == null) {
            instance = new ExpressionRegistryImpl();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Registry-API
    // -------------------------------------------------------------------------

    @Override
    public synchronized Set<String> getKeys() {
        // Builtins zuerst, dann User-Keys; stabile Reihenfolge für UI
        LinkedHashSet<String> all = new LinkedHashSet<String>();
        all.addAll(builtins.names());
        all.addAll(expressions.keySet());
        return Collections.unmodifiableSet(all);
    }

    @Override
    public synchronized Optional<String> getCode(String key) {
        // Builtins haben keinen User-Source
        String norm = normalize(key);
        if (builtins.contains(norm)) {
            return Optional.empty();
        }
        ExpressionEntry e = expressions.get(key);
        if (e == null) e = expressions.get(norm);
        return Optional.ofNullable(e != null ? e.getCode() : null);
    }

    @Override
    public synchronized void register(String key, String fullSourceCode) {
        // Verhindere, dass Builtin-Namen überschrieben werden
        String norm = normalize(key);
        if (builtins.contains(norm)) {
            // Optional: Fehlermeldung statt still ignorieren
            // throw new IllegalArgumentException("Name ist reserviert (Builtin): " + key);
            return;
        }
        ExpressionEntry existing = expressions.get(key);
        if (existing == null) {
            expressions.put(key, new ExpressionEntry(fullSourceCode, null));
        } else {
            existing.setCode(fullSourceCode != null ? fullSourceCode : "");
        }
    }

    @Override
    public synchronized void remove(String key) {
        // Builtins sind nicht löschbar
        String norm = normalize(key);
        if (builtins.contains(norm)) return;
        if (expressions.remove(key) == null) {
            expressions.remove(norm);
        }
    }

    @Override
    public synchronized void save() {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(expressions, writer);
        } catch (Exception e) {
            System.err.println("⚠ Fehler beim Speichern der Expressions: " + e.getMessage());
        }
    }

    @Override
    public synchronized void reload() {
        expressions.clear();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Map<String, ExpressionEntry> loaded = GSON.fromJson(reader, MAP_TYPE);
                if (loaded != null) {
                    expressions.putAll(loaded);
                }
            } catch (Exception e) {
                System.err.println("⚠ Fehler beim Laden der Expressions: " + e.getMessage());
            }
        }
        ExpressionExamples.ensureExamplesRegistered(this);
    }

    @Override
    public synchronized String evaluate(String key, List<String> params) throws Exception {
        String norm = normalize(key);

        // 1) Builtin? Direkt ausführen (ohne Compile)
        ExpressionFunction builtin = builtins.get(norm);
        if (builtin != null) {
            List<String> safeParams = params != null ? params : Collections.<String>emptyList();
            return builtin.invoke(safeParams, defaultContext());
        }

        // 2) User-Expression via Compile
        ExpressionEntry e = expressions.get(key);
        if (e == null) e = expressions.get(norm);
        String source = e != null ? e.getCode() : null;
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Kein Quelltext für Ausdruck: '" + key + "'");
        }

        if (!looksLikeFullClass(source)) {
            throw new IllegalArgumentException(
                    "Bitte vollständige Klasse im Stil der Beispiele angeben (implements Function<List<String>, String>)."
            );
        }

        String className = extractClassName(source, key);
        Object instance = compiler.compile(className, source, java.util.function.Function.class);

        java.lang.reflect.Method apply = instance.getClass().getMethod("apply", Object.class);
        Object result = apply.invoke(instance, params);
        return String.valueOf(result);
    }

    // -------------------------------------------------------------------------
    // IntelliSense-Unterstützung (optional für deinen Functions-Tab)
    // -------------------------------------------------------------------------

    /** Metadaten aller Builtins (z. B. für IntelliSense). */
    public synchronized Collection<FunctionMetadata> builtinMetadata() {
        return builtins.metadata();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FunctionContext defaultContext() {
        // Keep minimal; erweitere bei Bedarf um Services
        return new FunctionContext() {
            public String resolveVariable(String name) { return ""; }
            public Map<String, Object> services() { return Collections.emptyMap(); }
        };
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    /** Erkenne grob, ob es schon echter Klassencode ist. */
    private boolean looksLikeFullClass(String src) {
        String s = src == null ? "" : src.trim();
        return s.contains(" class ") || s.startsWith("class ")
                || s.contains(" interface ") || s.startsWith("interface ")
                || s.contains(" enum ") || s.startsWith("enum ");
    }

    /** (Unbenutzt) Wrapper-Code, falls du später doch Body-Snippets zulassen willst. */
    @SuppressWarnings("unused")
    private String buildWrapperSource(String className, String body) {
        return ""
                + "import java.util.*;\n"
                + "import java.util.function.*;\n"
                + "public class " + className + " implements Function<Object,Object> {\n"
                + "  private static String param(List<String> P, int i) {\n"
                + "    int idx = i - 1;\n"
                + "    return (P != null && idx >= 0 && idx < P.size()) ? String.valueOf(P.get(idx)) : \"\";\n"
                + "  }\n"
                + "  @SuppressWarnings(\"unchecked\")\n"
                + "  public Object apply(Object __args) {\n"
                + "    List<String> P = (__args instanceof List) ? (List<String>) __args : Collections.emptyList();\n"
                + "    String $1 = param(P,1);\n"
                + "    String $2 = param(P,2);\n"
                + "    String $3 = param(P,3);\n"
                + "    String $4 = param(P,4);\n"
                + "    String $5 = param(P,5);\n"
                + "    String $6 = param(P,6);\n"
                + "    String $7 = param(P,7);\n"
                + "    String $8 = param(P,8);\n"
                + "    String $9 = param(P,9);\n"
                + "    // ====== User-Code (Funktionsrumpf) ======\n"
                +          (body == null ? "" : body) + "\n"
                + "    // Falls kein return im Body vorhanden war:\n"
                + "    return \"\";\n"
                + "  }\n"
                + "}\n";
    }

    private File getSettingsFolder() {
        String home = System.getProperty("user.home", ".");
        File dir = new File(home, ".wd4j"); // Beispielordner
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private String extractClassName(String source, String fallback) {
        if (source != null) {
            String[] lines = source.split("\\R");
            for (int i = 0; i < lines.length; i++) {
                String t = lines[i].trim();
                if (t.startsWith("public class ")) {
                    String[] tokens = t.split("\\s+");
                    if (tokens.length >= 3) return tokens[2];
                }
            }
        }
        return "Expr_" + (fallback == null ? "_" : fallback.replaceAll("[^a-zA-Z0-9_$]", "_"));
    }

    @Override
    public synchronized ExpressionFunction get(String name) {
        String norm = normalize(name);

        // Zuerst Builtin-Funktionen prüfen
        ExpressionFunction builtin = builtins.get(norm);
        if (builtin != null) {
            return builtin; // Wenn gefunden, zurückgeben
        }

        // Falls nicht im Builtin-Katalog, schauen wir in den benutzerspezifischen Funktionen
        ExpressionEntry entry = expressions.get(name);
        if (entry == null) entry = expressions.get(norm);
        String sourceCode = entry != null ? entry.getCode() : null;

        if (sourceCode != null && !sourceCode.trim().isEmpty()) {
            // Wenn eine benutzerspezifische Funktion existiert, kompilieren und zurückgeben
            String className = extractClassName(sourceCode, norm);
            Object instance = null;
            try {
                instance = compiler.compile(className, sourceCode, Function.class);
            } catch (Exception e) {
                return null; // do not deliver broken functions
            }
            if (instance instanceof ExpressionFunction) {
                return (ExpressionFunction) instance;
            }
        }

        // Wenn keine Funktion gefunden, null zurückgeben
        return null;
    }

    @Override
    public synchronized FunctionMetadata getMetadata(String key) {
        ExpressionEntry e = expressions.get(key);
        if (e == null) e = expressions.get(normalize(key));
        FunctionMetadata m = e != null ? e.getMeta() : null;
        return m != null ? m : new FunctionMetadata("", "", Collections.<String>emptyList(), Collections.<String>emptyList());
    }

    @Override
    public synchronized void setMetadata(String key, FunctionMetadata meta) {
        if (key == null || key.trim().isEmpty()) return;
        ExpressionEntry e = expressions.get(key);
        if (e == null) e = expressions.get(normalize(key));
        if (e == null) {
            expressions.put(key, new ExpressionEntry("", meta));
        } else {
            e.setMeta(meta);
        }
    }

    // -------------------------------------------------------------------------
    // Persistenz-Datenträger für User-Expressions
    // -------------------------------------------------------------------------
    private static final class ExpressionEntry {
        private String code;
        private FunctionMetadata meta;

        ExpressionEntry() { /* for Gson */ }

        ExpressionEntry(String code, FunctionMetadata meta) {
            this.code = code != null ? code : "";
            setMeta(meta);
        }

        String getCode() { return code; }

        void setCode(String code) { this.code = code != null ? code : ""; }

        FunctionMetadata getMeta() { return meta; }

        void setMeta(FunctionMetadata meta) {
            if (meta == null) {
                this.meta = new FunctionMetadata("", "", Collections.<String>emptyList(), Collections.<String>emptyList());
            } else {
                List<String> names = meta.getParameterNames() != null ? meta.getParameterNames() : Collections.<String>emptyList();
                List<String> descs = meta.getParameterDescriptions() != null ? meta.getParameterDescriptions() : Collections.<String>emptyList();
                this.meta = new FunctionMetadata(
                        meta.getName() != null ? meta.getName() : "",
                        meta.getDescription() != null ? meta.getDescription() : "",
                        names,
                        descs
                );
            }
        }
    }
}
