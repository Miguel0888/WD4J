package de.bund.zrb.expressions.domain;

import de.bund.zrb.ui.celleditors.DescribedItem;
import java.util.Collections;
import java.util.List;

public interface ExpressionFunction extends DescribedItem {

    String invoke(List<String> args, FunctionContext ctx) throws FunctionExecutionException;

    FunctionMetadata metadata();

    @Override
    default String getDescription() {
        FunctionMetadata m = metadata();
        return (m != null && m.getDescription() != null) ? m.getDescription() : "";
    }

    /** Bridge for old reflection paths, but no longer required by the UI. */
    default Object getMetadata() { return metadata(); }

    @Override
    default List<String> getParamNames() {
        FunctionMetadata m = metadata();
        return m != null ? m.getParameterNames() : Collections.<String>emptyList();
    }

    @Override
    default List<String> getParamDescriptions() {
        FunctionMetadata m = metadata();
        return m != null ? m.getParameterDescriptions() : Collections.<String>emptyList();
    }
}
