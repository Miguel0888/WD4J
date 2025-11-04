package de.bund.zrb.expressions.domain;

import java.util.List;

public interface ExpressionFunction {
    // Execute function with raw argument strings; resolve variables outside or via context
    String invoke(List<String> args, FunctionContext ctx) throws FunctionExecutionException;

    // Metadata is separate to keep SRP
    FunctionMetadata metadata();
}
