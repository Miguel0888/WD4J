package de.bund.zrb.runtime;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Provide a simple singleton-backed ExpressionRegistry.
 *
 * Intent:
 * - Keep function definitions in memory (Map<String,String>).
 * - Allow UI to edit them.
 * - Allow runtime to evaluate them by name.
 *
 * Note:
 * - This is a minimal, self-contained implementation to make the UI usable
 *   in isolation in your new project.
 * - Replace evaluate(...) with your domain logic if you already have
 *   a richer ExpressionRegistryImpl in the original codebase.
 */
public class ExpressionRegistryImpl implements ExpressionRegistry {

    private static final ExpressionRegistryImpl INSTANCE = new ExpressionRegistryImpl();

    // Persist to a file under user.home by default for simplicity
    private static final String REGISTRY_FILENAME = ".expressions-registry.json";

    private final Map<String, String> codeByKey = new LinkedHashMap<String, String>();
    private final Gson gson = new Gson();

    private ExpressionRegistryImpl() {
        loadFromDisk();
        ensureSampleFunctions();
    }

    public static ExpressionRegistryImpl getInstance() {
        return INSTANCE;
    }

    public synchronized List<String> getKeys() {
        return new ArrayList<String>(codeByKey.keySet());
    }

    public synchronized Optional<String> getCode(String key) {
        if (!codeByKey.containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(codeByKey.get(key));
    }

    public synchronized void register(String key, String code) {
        codeByKey.put(key, code != null ? code : "");
    }

    public synchronized void remove(String key) {
        codeByKey.remove(key);
    }

    /**
     * Evaluate the named function.
     *
     * Important:
     * - This method is the execution hook that FunctionExpression will call
     *   at resolve-time in the workflow / cucumber step.
     * - Do not precompute time-sensitive values (e.g. OTP) elsewhere.
     *   Always compute here so it is lazy.
     *
     * Demo rules:
     * - "otp": Generate a time-based one-time code string (dummy impl here).
     * - Otherwise: Treat stored code as a template string that may refer to $1, $2, ...
     *   Replace $N with params[N-1].
     *
     * In your real system you most likely already have more advanced logic
     * (e.g. compiled Java / scripting). Plug it in here.
     */
    public synchronized String evaluate(String key, List<String> params) {
        if ("otp".equals(key)) {
            return generateOtp();
        }

        String code = codeByKey.get(key);
        if (code == null) {
            throw new IllegalArgumentException("Unknown function: " + key);
        }

        String resolved = code;
        for (int i = 0; i < params.size(); i++) {
            String token = "$" + (i + 1);
            String value = params.get(i) != null ? params.get(i) : "";
            resolved = resolved.replace(token, value);
        }
        return resolved;
    }

    /**
     * Persist registry state as JSON on disk.
     */
    public synchronized void save() {
        try {
            File file = getRegistryFile();
            FileWriter fw = new FileWriter(file);
            try {
                gson.toJson(codeByKey, fw);
            } finally {
                fw.close();
            }
        } catch (Exception ex) {
            // Do not throw here to keep UI responsive. Just log to stderr.
            System.err.println("Failed to save ExpressionRegistry: " + ex.getMessage());
        }
    }

    /**
     * Ensure there are some demo functions to make the UI meaningful on first start.
     * Add otp() if missing so users can test lazy / time critical behavior immediately.
     */
    private void ensureSampleFunctions() {
        if (!codeByKey.containsKey("otp")) {
            // Store example "implementation code" (for display in the editor).
            // Real generation happens in evaluate("otp", ...)
            codeByKey.put("otp",
                    "// Generate a one-time password (OTP)\n" +
                            "// This code is evaluated lazily at runtime.\n" +
                            "return otp();"
            );
        }
        if (!codeByKey.containsKey("wrap")) {
            codeByKey.put("wrap",
                    "// Simple demo wrapper\n" +
                            "// $1 will be replaced with first parameter, $2 with second.\n" +
                            "return \"[\" + $1 + \"|\" + $2 + \"]\";"
            );
        }
    }

    private void loadFromDisk() {
        try {
            File file = getRegistryFile();
            if (!file.exists()) {
                return;
            }
            FileReader fr = new FileReader(file);
            try {
                Type mapType = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> loaded = gson.fromJson(fr, mapType);
                if (loaded != null) {
                    codeByKey.clear();
                    codeByKey.putAll(loaded);
                }
            } finally {
                fr.close();
            }
        } catch (Exception ex) {
            System.err.println("Failed to load ExpressionRegistry: " + ex.getMessage());
        }
    }

    private File getRegistryFile() {
        String home = System.getProperty("user.home", ".");
        return new File(home, REGISTRY_FILENAME);
    }

    /**
     * Generate a time-based one-time code.
     * Replace this with your real TOTP/HOTP logic.
     *
     * Demo implementation:
     * - Use current timestamp minute + simple hash to mimic "fresh each time".
     */
    private String generateOtp() {
        long nowMillis = System.currentTimeMillis();
        long window = nowMillis / 30_000L; // 30s window
        int hash = (int)(window ^ (window >>> 32));
        int normalized = Math.abs(hash % 1_000_000);

        // Format as 6 digits with leading zeros
        String formatted = String.format("%06d", normalized);

        // Add debug-friendly timestamp info (for panel preview)
        String ts = new SimpleDateFormat("HH:mm:ss").format(new Date(nowMillis));
        return formatted + " (at " + ts + ")";
    }
}
