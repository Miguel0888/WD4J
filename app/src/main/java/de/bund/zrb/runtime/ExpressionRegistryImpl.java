// de/bund/zrb/runtime/ExpressionRegistryImpl.java
package de.bund.zrb.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.bund.zrb.compiler.InMemoryJavaCompiler;
import de.bund.zrb.ui.settings.ExpressionExamples;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

public class ExpressionRegistryImpl implements ExpressionRegistry {

    private static final String FILE_NAME = "expressions.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private static ExpressionRegistryImpl instance;

    private final Map<String, String> expressions = new LinkedHashMap<>();
    private final File file;
    private final InMemoryJavaCompiler compiler = new InMemoryJavaCompiler();

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

    @Override
    public synchronized Set<String> getKeys() {
        return expressions.keySet();
    }

    @Override
    public synchronized Optional<String> getCode(String key) {
        return Optional.ofNullable(expressions.get(key));
    }

    @Override
    public synchronized void register(String key, String fullSourceCode) {
        expressions.put(key, fullSourceCode != null ? fullSourceCode : "");
    }

    @Override
    public synchronized void remove(String key) {
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
        String source = expressions.get(key);
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Kein Quelltext für Ausdruck: '" + key + "'");
        }

        // Comment: Require users to provide a full class (see examples); avoid wrappers
        if (!looksLikeFullClass(source)) {
            throw new IllegalArgumentException(
                    "Bitte vollständige Klasse im Stil der Beispiele angeben (implements Function<List<String>, String>)."
            );
        }

        String className = extractClassName(source, key);
        Object instance = compiler.compile(className, source, java.util.function.Function.class);

        // Comment: Type erasure makes apply(Object) the reflective signature
        java.lang.reflect.Method apply = instance.getClass().getMethod("apply", Object.class);
        Object result = apply.invoke(instance, params);
        return String.valueOf(result);
    }

    /** Erkenne grob, ob es schon echter Klassencode ist. */
    private boolean looksLikeFullClass(String src) {
        String s = src.trim();
        // sehr simpel: wenn irgendwo "class " oder "interface " oder "enum " vorkommt, nehmen wir es als voll.
        return s.contains(" class ") || s.startsWith("class ")
                || s.contains(" interface ") || s.startsWith("interface ")
                || s.contains(" enum ") || s.startsWith("enum ");
    }

    /** Erzeugt eine minimal lauffähige Klasse um den Body. */
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
                +          body + "\n"
                + "    // Falls kein return im Body vorhanden war:\n"
                + "    return \"\";\n"
                + "  }\n"
                + "}\n";
    }

    // Comment: Sanitize string to a valid Java identifier; keep intent readable
    private String toValidJavaIdentifier(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "_";
        }

        String s = input.trim();

        // Build result char by char according to Java identifier rules
        StringBuilder out = new StringBuilder(s.length());

        // Handle first character
        char c0 = s.charAt(0);
        if (Character.isJavaIdentifierStart(c0)) {
            out.append(c0);
        } else {
            // Comment: Prepend underscore if first char is invalid
            out.append('_');
            if (Character.isJavaIdentifierPart(c0)) {
                out.append(c0);
            } else {
                out.append('_');
            }
        }

        // Handle remaining characters
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(Character.isJavaIdentifierPart(c) ? c : '_');
        }

        // Optional: Collapse multiple underscores and trim edges
        String cleaned = out.toString().replaceAll("_+", "_");
        if (cleaned.equals("_")) {
            // Comment: Avoid returning a bare underscore if possible
            cleaned = "_X";
        }
        return cleaned;
    }

    private File getSettingsFolder() {
        // Passe das an deine Settings-Location an
        // Falls du bereits SettingsService/SettingsHelper hast, binde das hier ein.
        String home = System.getProperty("user.home", ".");
        File dir = new File(home, ".wd4j"); // Beispielordner
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private String extractClassName(String source, String fallback) {
        for (String line : source.split("\\R")) {
            String t = line.trim();
            if (t.startsWith("public class ")) {
                String[] tokens = t.split("\\s+");
                if (tokens.length >= 3) return tokens[2];
            }
        }
        return "Expr_" + fallback.replaceAll("[^a-zA-Z0-9_$]", "_");
    }
}
