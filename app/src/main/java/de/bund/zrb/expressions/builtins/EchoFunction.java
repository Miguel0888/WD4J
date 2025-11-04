package de.bund.zrb.expressions.builtins;

import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionContext;
import de.bund.zrb.expressions.domain.FunctionExecutionException;
import de.bund.zrb.expressions.domain.FunctionMetadata;

import java.util.Arrays;
import java.util.List;

public final class EchoFunction implements ExpressionFunction {

    private static final FunctionMetadata META =
            new FunctionMetadata("Echo", "Echo the concatenation of arguments", Arrays.asList("value", "more..."));

    public String invoke(List<String> args, FunctionContext ctx) throws FunctionExecutionException {
        // Keep it simple: join with space
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(args.get(i));
        }
        return sb.toString();
    }

    public FunctionMetadata metadata() { return META; }
}
