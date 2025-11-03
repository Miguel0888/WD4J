package de.bund.zrb.service;

/**
 * Wrap checked Exceptions thrown inside lambdas so they can bubble up as RuntimeException.
 * Preserve the original cause for accurate error reporting at upper layers.
 */
final class ActionEvaluationRuntimeException extends RuntimeException {
    ActionEvaluationRuntimeException(Throwable cause) {
        super(cause);
    }
}
