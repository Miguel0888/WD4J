package de.bund.zrb.expressions.builtins.tooling;

import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionContext;
import de.bund.zrb.expressions.domain.FunctionExecutionException;
import de.bund.zrb.expressions.domain.FunctionMetadata;

import java.util.*;

public final class ToolExpressionFunction implements ExpressionFunction {

    public interface Invoker {
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

    public String invoke(List<String> args, FunctionContext ctx) throws FunctionExecutionException {
        try {
            List<String> a = (args != null) ? args : Collections.<String>emptyList();
            if (a.size() < minArity) {
                throw new FunctionExecutionException("Missing argument(s). Expected at least " + minArity + " but got " + a.size());
            }
            if (maxArity >= 0 && a.size() > maxArity) {
                throw new FunctionExecutionException("Too many arguments. Expected at most " + maxArity + " but got " + a.size());
            }
            return String.valueOf(invoker.invoke(a, ctx));
        } catch (FunctionExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new FunctionExecutionException("Tool invocation failed: " + e.getMessage(), e);
        }
    }

    public FunctionMetadata metadata() { return meta; }

    // helpers
    public static FunctionMetadata meta(String name, String desc, List<String> params) {
        return new FunctionMetadata(name, desc, params);
    }
    public static List<String> params(String p1) {
        List<String> l = new ArrayList<String>(1); l.add(p1); return l;
    }
    public static List<String> params(String p1, String p2) {
        List<String> l = new ArrayList<String>(2); l.add(p1); l.add(p2); return l;
    }
    public static List<String> params(String p1, String p2, String p3) {
        List<String> l = new ArrayList<String>(3); l.add(p1); l.add(p2); l.add(p3); return l;
    }
}
