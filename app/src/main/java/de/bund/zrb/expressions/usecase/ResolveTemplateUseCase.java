package de.bund.zrb.expressions.usecase;

import de.bund.zrb.expressions.domain.ResolvableExpression;
import de.bund.zrb.expressions.domain.UnresolvedSymbolException;
import de.bund.zrb.expressions.engine.ExpressionParser;
import de.bund.zrb.expressions.runtime.ResolutionContext;

/**
 * Resolve an entire template string with embedded {{...}} expressions
 * into a final String.
 *
 * Intent:
 * - Provide a single entry point for test steps (Cucumber, etc.).
 * - Keep calling code simple and clean.
 *
 * Usage example in a step:
 *
 *   ResolveTemplateUseCase useCase =
 *       new ResolveTemplateUseCase(myResolutionContext);
 *
 *   String finalText = useCase.resolveTemplate(
 *       "User {{userName}} logs in with OTP {{otp()}}"
 *   );
 *
 *   // Now use finalText against UI or API.
 */
public class ResolveTemplateUseCase {

    private final ExpressionParser parser = new ExpressionParser();
    private final ResolutionContext resolutionContext;

    public ResolveTemplateUseCase(ResolutionContext resolutionContext) {
        this.resolutionContext = resolutionContext;
    }

    /**
     * Parse and resolve the given template.
     * Return the resolved string.
     */
    public String resolveTemplate(String template) throws UnresolvedSymbolException {
        ResolvableExpression expr = parser.parseTemplate(template);
        Object result = expr.resolve(resolutionContext);
        return result == null ? "" : result.toString();
    }
}
