package de.bund.zrb.compiler;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * A custom JavaFileObject that stores compiled class data in memory.
 * This allows for dynamic compilation and retrieval of classes without writing to disk.
 */
public class MemoryJavaFileObject extends SimpleJavaFileObject {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    public MemoryJavaFileObject(String className, Kind kind) {
        super(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind);
    }

    @Override
    public OutputStream openOutputStream() {
        return outputStream;
    }

    public byte[] getBytes() {
        return outputStream.toByteArray();
    }
}
