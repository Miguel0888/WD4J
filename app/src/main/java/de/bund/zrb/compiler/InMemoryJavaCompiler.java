package de.bund.zrb.compiler;

import javax.tools.*;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.ServiceLoader;

public class InMemoryJavaCompiler {

    private static JavaCompiler compiler;

    // Synchronized to ensure thread-safe lazy initialization
    private static synchronized JavaCompiler getCompiler() {
        if (compiler == null) {
            // Bevorzugt: System JavaCompiler (benötigt JDK, nicht nur JRE)
            compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                // Fallback: Versuch über ServiceLoader
                for (JavaCompiler c : ServiceLoader.load(JavaCompiler.class)) {
                    compiler = c;
                    break;
                }
            }
            if (compiler == null) {
                throw new IllegalStateException("Kein JavaCompiler verfügbar. Starte die IDE mit einer JDK (JAVA_HOME auf JDK setzen, nicht JRE).");
            }
        }
        return compiler;
    }

    public <T> T compile(String className, String sourceCode, Class<T> expectedType) throws Exception {
        JavaCompiler javaCompiler = getCompiler();

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager stdFileManager = javaCompiler.getStandardFileManager(diagnostics, Locale.getDefault(), null);
        MemoryJavaFileManager fileManager = new MemoryJavaFileManager(stdFileManager);

        JavaFileObject sourceFile = new JavaSourceFromString(className, sourceCode);

        // Basis-Optionen: -Xlint:unchecked abschalten, -proc:none (falls keine Annotationen notwendig)
        Iterable<String> options = Arrays.asList("-g", "-classpath", System.getProperty("java.class.path"), "-proc:none");

        JavaCompiler.CompilationTask task = javaCompiler.getTask(
                null,
                fileManager,
                diagnostics,
                options,
                null,
                Collections.singletonList(sourceFile)
        );

        boolean success = task.call();
        if (!success) {
            StringWriter sw = new StringWriter();
            for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                sw.append(d.getKind().name())
                  .append(" [").append(String.valueOf(d.getLineNumber())).append(":")
                  .append(String.valueOf(d.getColumnNumber())).append("] ")
                  .append(String.valueOf(d.getMessage(Locale.getDefault())))
                  .append("\n");
            }
            throw new IllegalStateException("Compilation failed:\n" + sw);
        }

        ClassLoader classLoader = fileManager.getClassLoader(null);
        Class<?> clazz = classLoader.loadClass(className);
        Object instance = clazz.getDeclaredConstructor().newInstance();

        if (!expectedType.isInstance(instance)) {
            throw new ClassCastException("Compiled class does not implement expected type: " + expectedType.getName());
        }
        return expectedType.cast(instance);
    }
}
