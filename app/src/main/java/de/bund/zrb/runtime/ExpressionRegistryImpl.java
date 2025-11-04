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

/**
 * Vereinigt Benutzer-Expressions (kompiliert zur Laufzeit) und eingebaute Funktionen (Builtins, kein Compile).
 * - Builtins werden über BuiltinFunctionCatalog bereitgestellt.
 * - evaluate(..) ruft zuerst Builtins direkt auf, danach ggf. die User-Implementierung via Compiler.
 * - Builtins tauchen in getKeys() auf, sind nicht löschbar und liefern bei getCode() kein Optional mit Source.
 */
public class ExpressionRegistryImpl implements ExpressionRegistry {

    private static final String FILE_NAME = "expressions.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private static ExpressionRegistryImpl instance;

    // Benutzerdefinierte Expressions (Name -> Source)
    private final Map<String, String> expressions = new LinkedHashMap<String, String>();
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
        return Optional.ofNullable(expressions.get(key));
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
        expressions.put(key, fullSourceCode != null ? fullSourceCode : "");
    }

    @Override
    public synchronized void remove(String key) {
        // Builtins sind nicht löschbar
        String norm = normalize(key);
        if (builtins.contains(norm)) return;
        expressions.remove(key);
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
                Map<String, String> loaded = GSON.fromJson(reader, MAP_TYPE);
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
        String source = expressions.get(key);
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
}
