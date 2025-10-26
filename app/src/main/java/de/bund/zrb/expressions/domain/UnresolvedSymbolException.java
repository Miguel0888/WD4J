package de.bund.zrb.expressions.domain;

/**
 * Indicate that a variable or nested expression cannot be resolved.
 */
public class UnresolvedSymbolException extends Exception {

    private final String symbolName;

    public UnresolvedSymbolException(String symbolName) {
        super("Unresolved symbol: " + symbolName);
        this.symbolName = symbolName;
    }

    public String getSymbolName() {
        return symbolName;
    }
}
