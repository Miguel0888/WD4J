package de.bund.zrb.compiler;

import de.bund.zrb.MemoryJavaFileObject;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.util.HashMap;
import java.util.Map;

/**
 * A custom JavaFileManager that stores compiled classes in memory.
 * This allows for dynamic compilation and retrieval of classes without writing to disk.
 */
public class MemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    private final Map<String, MemoryJavaFileObject> compiledClasses = new HashMap<>();

    public MemoryJavaFileManager(JavaFileManager fileManager) {
        super(fileManager);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className,
                                               JavaFileObject.Kind kind, FileObject sibling) {
        MemoryJavaFileObject fileObject = new MemoryJavaFileObject(className, kind);
        compiledClasses.put(className, fileObject);
        return fileObject;
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                MemoryJavaFileObject file = compiledClasses.get(name);
                if (file != null) {
                    byte[] bytes = file.getBytes();
                    return defineClass(name, bytes, 0, bytes.length);
                }
                return super.findClass(name);
            }
        };
    }
}
