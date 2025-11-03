package de.bund.zrb.compiler;

import com.sun.tools.javac.api.JavacTool;
import de.bund.zrb.JavaSourceFromString;
import de.bund.zrb.MemoryJavaFileManager;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import java.util.Collections;

public class InMemoryJavaCompiler {

    private static JavaCompiler compiler;

    // Synchronized to ensure thread-safe lazy initialization
    private static synchronized JavaCompiler getCompiler() {
        if (compiler == null) {
            compiler = JavacTool.create(); // fallback attempt
            if (compiler == null) {
                throw new IllegalStateException("JavaCompiler konnte nicht geladen werden. Stelle sicher, dass tools.jar im Classpath ist.");
            }
        }
        return compiler;
    }
    public <T> T compile(String className, String sourceCode, Class<T> expectedType) throws Exception {
        JavaCompiler javaCompiler = getCompiler();

        MemoryJavaFileManager fileManager = new MemoryJavaFileManager(
                javaCompiler.getStandardFileManager(null, null, null));

        JavaFileObject sourceFile = new JavaSourceFromString(className, sourceCode);
        boolean success = javaCompiler.getTask(null, fileManager, null, null, null, Collections.singletonList(sourceFile)).call();

        if (!success) {
            throw new IllegalStateException("Compilation failed");
        }

        ClassLoader classLoader = fileManager.getClassLoader(null);
        Class<?> clazz = classLoader.loadClass(className);
        Object instance = clazz.newInstance();

        if (!expectedType.isInstance(instance)) {
            throw new ClassCastException("Compiled class does not implement expected type: " + expectedType.getName());
        }

        return expectedType.cast(instance);
    }
}
