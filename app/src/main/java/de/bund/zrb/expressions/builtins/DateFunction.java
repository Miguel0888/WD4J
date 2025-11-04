package de.bund.zrb.expressions.builtins;

import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionContext;
import de.bund.zrb.expressions.domain.FunctionExecutionException;
import de.bund.zrb.expressions.domain.FunctionMetadata;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public final class DateFunction implements ExpressionFunction {

    private static final FunctionMetadata META =
            new FunctionMetadata(
                    "Date",
                    "Return current date/time. If a pattern is given, format the date using java.text.SimpleDateFormat.",
                    Arrays.asList("pattern?"),
                    Arrays.asList("Optional SimpleDateFormat pattern, e.g. 'yyyy-MM-dd HH:mm:ss'")
            );

    @Override
    public String invoke(List<String> args, FunctionContext ctx) throws FunctionExecutionException {
        // Format or return epoch millis
        Date now = new Date();
        if (args != null && args.size() > 0) {
            String pattern = args.get(0);
            if (pattern != null && pattern.length() > 0) {
                try {
                    SimpleDateFormat fmt = new SimpleDateFormat(pattern);
                    return fmt.format(now);
                } catch (IllegalArgumentException ex) {
                    throw new FunctionExecutionException("Invalid date pattern: " + pattern, ex);
                }
            }
        }
        return Long.toString(now.getTime());
    }

    @Override
    public FunctionMetadata metadata() {
        return META;
    }

    // nichts weiter nötig: getDescription() und getMetadata() kommen als Default aus ExpressionFunction
}
