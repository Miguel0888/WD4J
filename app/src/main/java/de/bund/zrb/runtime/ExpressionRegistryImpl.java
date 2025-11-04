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
// Falls du bereits Builtins angelegt hast (z. B. DateFunction), importiere sie im BuiltinFunctionCatalog.

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Registry vereinigt:
 * - Benutzerdefinierte Expressions (persistiert und zur Laufzeit kompiliert)
 * - Eingebaute Funktionen (ohne Compile, direkter Aufruf)
 *
 * Design-Notizen:
 * - Favorisiere Builtins: evaluate() dispatcht zuerst auf den Builtin-Katalog.
 * - Builtins erscheinen in getKeys(), sind aber nicht löschbar und haben keinen getCode()-Inhalt.
 * - Halte API kompatibel zu deiner bisherigen Registry.
 */
public class ExpressionRegistryImpl implements ExpressionRegistry {

    private static final String FILE_NAME = "expressions.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private static ExpressionRegistryImpl instance;

    // --- Benutzerdefinierte Expressions (Name -> Source) ---
    private final Map<String, String> expressions = new LinkedHashMap<String, String>();
    private final File file;
    private final InMemoryJavaCompiler compiler = new InMemoryJavaCompiler();

    // --- Eingebaute Funktionen (ohne Compile) ---
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
        // Combine builtins + user expressions (stabile Reihenfolge)
        LinkedHashSet<String> all = new LinkedHashSet<String>();
        all.addAll(builtins.names());        // zuerst Builtins
        all.addAll(expressions.keySet());    // dann User
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
        // Erlaube Namensüberschreibung auf User-Seite (aber nicht für Builtin)
        String norm = normalize(key);
        if (builtins.contains(norm)) {
            // Optional: Fehler werfen, wenn du Builtin-Name schützen willst
            // throw new IllegalArgumentException("Name ist reserviert (Builtin): " + key);
            // Hier: einfach ignorieren
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

        // --- 1) Builtin? Dann direkt aufrufen (kein Compile) ---
        ExpressionFunction builtin = builtins.get(norm);
        if (builtin != null) {
            List<String> safeParams = params != null ? params : Collections.<String>emptyList();
            return builtin.invoke(safeParams, defaultContext());
        }

        // --- 2) User-defined via Compile ---
        String source = expressions.get(key);
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Kein Quelltext für Ausdruck: '" + key + "'");
        }

        // Require "volle Klasse" (dein bisheriges Verhalten)
        if (!looksLikeFullClass(source)) {
            throw new IllegalArgumentException(
                    "Bitte vollständige Klasse im Stil der Beispiele angeben (implements Function<List<String>, String>)."
            );
        }

        String className = extractClassName(source, key);
        Object instance = compiler.compile(className, source, java.util.function.Function.class);

        // Type erasure -> apply(Object)
        java.lang.reflect.Method apply = instance.getClass().getMethod("apply", Object.class);
        Object result = apply.invoke(instance, params);
        return String.valueOf(result);
    }

    // -------------------------------------------------------------------------
    // Zusätzliche, nützliche API für IntelliSense (optional)
    // -------------------------------------------------------------------------

    /** Liefere Metadaten aller Builtins (z. B. für den Functions-Tab). */
    public synchronized Collection<FunctionMetadata> builtinMetadata() {
        return builtins.metadata();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Provide a minimal default context. Extend when services are needed. */
    private FunctionContext defaultContext() {
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
        // sehr simpel: wenn irgendwo "class " oder "interface " oder "enum " vorkommt, nehmen wir es als voll.
        return s.contains(" class ") || s.startsWith("class ")
                || s.contains(" interface ") || s.startsWith("interface ")
                || s.contains(" enum ") || s.startsWith("enum ");
    }

    /** Erzeuge eine minimal lauffähige Klasse um einen Body (wird aktuell nicht genutzt). */
    @SuppressWarnings("unused")
    private String buildWrapperSource(String className, String body) {
        // Hinweis:
        // - params kommen als List<String> rein (Variable "P")
        // - $1..$9 werden als bequeme Aliasse gesetzt
        // - param(i) ist 1-basiert, liefert "" wenn nicht vorhanden
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
        // Passe das an deine Settings-Location an
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

    // =========================================================================
    // BuiltinFunctionCatalog (lokal eingebettet, kann auch in eigene Datei)
    // =========================================================================
    /**
     * Katalog der fest implementierten Funktionen.
     * - Registriere hier alle Builtins (Date, Echo, …).
     * - Liefere Implementierungen und Metadaten für IntelliSense.
     */
    static final class BuiltinFunctionCatalog {

        private final Map<String, ExpressionFunction> byName = new LinkedHashMap<String, ExpressionFunction>();
        private final Map<String, FunctionMetadata> metaByName = new LinkedHashMap<String, FunctionMetadata>();

        BuiltinFunctionCatalog() {
            // TODO: Registriere hier deine Builtins:
            // register(new de.bund.zrb.expressions.builtins.DateFunction());
            // register(new de.bund.zrb.expressions.builtins.EchoFunction());
            // ...
        }

        Set<String> names() {
            return Collections.unmodifiableSet(byName.keySet());
        }

        boolean contains(String name) {
            return name != null && byName.containsKey(name);
        }

        ExpressionFunction get(String name) {
            return name == null ? null : byName.get(name);
        }

        Collection<FunctionMetadata> metadata() {
            return Collections.unmodifiableCollection(metaByName.values());
        }

        private void register(ExpressionFunction fn) {
            if (fn == null || fn.metadata() == null || fn.metadata().getName() == null) return;
            String key = normalize(fn.metadata().getName());
            byName.put(key, fn);
            metaByName.put(key, fn.metadata());
        }

        private String normalize(String s) {
            return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
        }
    }
}
