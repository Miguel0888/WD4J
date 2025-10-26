package de.bund.zrb.expressions.engine;

/**
 * Represent a lexical token extracted from the template.
 *
 * TEXT:
 *   literal text outside of any {{ ... }} region
 *
 * EXPR:
 *   content inside a {{ ... }} block (without the curly braces)
 */
public class Token {

    public static enum Type {
        TEXT,
        EXPR
    }

    private final Type type;
    private final String content;

    public Token(Type type, String content) {
        this.type = type;
        this.content = content;
    }

    public Type getType() {
        return type;
    }

    public String getContent() {
        return content;
    }
}
