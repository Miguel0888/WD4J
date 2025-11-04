package de.bund.zrb.expressions.builtins.tooling;

import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.service.ToolsRegistry;

import java.util.*;

public final class ToolFunctionsCollector {

    private ToolFunctionsCollector() {}

    /** Sammle alle ExpressionFunctions aus allen Tools, die BuiltinTool implementieren. */
    public static Collection<ExpressionFunction> collectFrom(ToolsRegistry reg) {
        List<ExpressionFunction> out = new ArrayList<ExpressionFunction>();

        // Addiere hier alle Tools aus der Registry
        add(out, reg.screenshotTool());
        add(out, reg.twoFaTool());
        add(out, reg.navigationTool());
        add(out, reg.loginTool());
        add(out, reg.notificationTool());

        return out;
    }

    private static void add(List<ExpressionFunction> out, Object maybeBuiltinTool) {
        if (maybeBuiltinTool instanceof BuiltinTool) {
            BuiltinTool b = (BuiltinTool) maybeBuiltinTool;
            Collection<ExpressionFunction> c = b.builtinFunctions();
            if (c != null) out.addAll(c);
        }
    }
}
