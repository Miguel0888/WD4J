package de.bund.zrb.expressions.domain;

public final class FunctionExecutionException extends Exception {
    public FunctionExecutionException(String message) { super(message); }
    public FunctionExecutionException(String message, Throwable cause) { super(message, cause); }
}
