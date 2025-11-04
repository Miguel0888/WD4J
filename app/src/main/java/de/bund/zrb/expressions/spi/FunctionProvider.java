package de.bund.zrb.expressions.spi;

import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionMetadata;

import java.util.Collection;

public interface FunctionProvider {
    // Return known functions (metadata + instance supplier inside the ExpressionFunction)
    Collection<ExpressionFunction> functions();

    // Lightweight access to metadata (for IntelliSense), optional
    Collection<FunctionMetadata> metadata();
}
