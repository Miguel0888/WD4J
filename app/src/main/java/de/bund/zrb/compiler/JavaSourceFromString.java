package de.bund.zrb.compiler;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

public class JavaSourceFromString extends SimpleJavaFileObject {

    private final String code;

    public JavaSourceFromString(String name, String code) {
        super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }
}
