package de.bund.zrb.expressions.builtins;

import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionContext;
import de.bund.zrb.expressions.domain.FunctionExecutionException;
import de.bund.zrb.expressions.domain.FunctionMetadata;

import java.text.SimpleDateFormat;
import java.util.*;

public final class DateFunction implements ExpressionFunction {

    private static final FunctionMetadata META =
            new FunctionMetadata("Date", "Return current date/time with optional pattern", Arrays.asList("pattern?"), null);

    public String invoke(List<String> args, FunctionContext ctx) throws FunctionExecutionException {
        Date now = new Date();
        if (args.size() > 0 && args.get(0) != null && args.get(0).length() > 0) {
            try {
                SimpleDateFormat fmt = new SimpleDateFormat(args.get(0));
                return fmt.format(now);
            } catch (IllegalArgumentException ex) {
                throw new FunctionExecutionException("Invalid date pattern: " + args.get(0), ex);
            }
        }
        return Long.toString(now.getTime());
    }

    public FunctionMetadata metadata() { return META; }
}
