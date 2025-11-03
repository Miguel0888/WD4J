// de/bund/zrb/runtime/ExpressionRegistryImpl.java
package de.bund.zrb.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.bund.zrb.compiler.InMemoryJavaCompiler;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Method;
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
    public synchronized List<String> getKeys() {
        return new ArrayList<>(expressions.keySet());
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
    }

    @Override
    public synchronized String evaluate(String key, List<String> args) throws Exception {
        String source = expressions.get(key);
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Kein Quelltext für Ausdruck: '" + key + "'");
        }

        String className = extractClassName(source, key);

        // Wir erwarten eine Klasse, die java.util.function.Function implementiert.
        Object instance = compiler.compile(className, source, java.util.function.Function.class);

        Method apply = instance.getClass().getMethod("apply", Object.class);
        Object result = apply.invoke(instance, args);

        return String.valueOf(result);
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
