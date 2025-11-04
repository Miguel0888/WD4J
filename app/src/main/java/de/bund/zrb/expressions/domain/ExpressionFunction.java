package de.bund.zrb.expressions.domain;

import de.bund.zrb.ui.celleditors.DescribedItem;

import java.util.List;

public interface ExpressionFunction extends DescribedItem {

    // --- UseCase/Domain API ---
    String invoke(List<String> args, FunctionContext ctx) throws FunctionExecutionException;

    // Keep SRP: metadata carried here
    FunctionMetadata metadata();

    // --- Bridge for UI/reflection (Default-Methoden) ---

    @Override
    default String getDescription() {
        // Delegate to metadata description
        FunctionMetadata m = metadata();
        return m != null && m.getDescription() != null ? m.getDescription() : "";
    }

    /**
     * Expose legacy "getMetadata()" name so existing reflection finds it.
     * Return type is Object on purpose to avoid coupling UI to domain type.
     */
    default Object getMetadata() {
        return metadata();
    }
}
