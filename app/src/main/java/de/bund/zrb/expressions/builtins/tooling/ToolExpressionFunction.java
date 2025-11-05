package de.bund.zrb.expressions.builtins.tooling;

import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionContext;
import de.bund.zrb.expressions.domain.FunctionExecutionException;
import de.bund.zrb.expressions.domain.FunctionMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bridge zwischen Tools und Expression-Engine.
 */
public final class ToolExpressionFunction implements ExpressionFunction {

    /** Strategy zum Ausführen der Funktion. */
    public interface Invoker {
        /** Execute tool with args and return textual result. */
        String invoke(List<String> args, FunctionContext ctx) throws Exception;
    }

    private final FunctionMetadata meta;
    private final Invoker invoker;
    private final int minArity;
    private final int maxArity; // -1 = unbounded

    public ToolExpressionFunction(FunctionMetadata meta, int minArity, int maxArity, Invoker invoker) {
        if (meta == null) throw new IllegalArgumentException("meta required");
        if (invoker == null) throw new IllegalArgumentException("invoker required");
        this.meta = meta;
        this.invoker = invoker;
        this.minArity = Math.max(0, minArity);
        this.maxArity = maxArity;
    }

    // -------------------------------- API --------------------------------

    public String invoke(List<String> args, FunctionContext ctx) throws FunctionExecutionException {
        try {
            List<String> a = (args != null) ? args : Collections.<String>emptyList();
            if (a.size() < minArity) {
                throw new FunctionExecutionException(
                        "Missing argument(s). Expected at least " + minArity + " but got " + a.size());
            }
            if (maxArity >= 0 && a.size() > maxArity) {
                throw new FunctionExecutionException(
                        "Too many arguments. Expected at most " + maxArity + " but got " + a.size());
            }
            return String.valueOf(invoker.invoke(a, ctx));
        } catch (FunctionExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new FunctionExecutionException("Tool invocation failed: " + e.getMessage(), e);
        }
    }

    public FunctionMetadata metadata() { return meta; }

    // --------------------------- Helpers/Factory ---------------------------

    /** Create metadata with names and parameter doc. */
    public static FunctionMetadata meta(String name, String desc, List<String> params, List<String> paramDescs) {
        return new FunctionMetadata(name, desc, params, paramDescs);
    }

    /**
     * Build parameter name list. Supports any arity:
     * <pre>
     * params()                       -> []
     * params("a")                    -> ["a"]
     * params("a","b","c","d",...)    -> [...]
     * </pre>
     */
    public static List<String> params(String... names) {
        if (names == null || names.length == 0) {
            return new ArrayList<String>(0);
        }
        List<String> l = new ArrayList<String>(names.length);
        for (int i = 0; i < names.length; i++) {
            l.add(names[i]);
        }
        return l;
    }
}
