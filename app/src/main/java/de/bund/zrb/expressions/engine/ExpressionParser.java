package de.bund.zrb.expressions.engine;

import de.bund.zrb.expressions.domain.ResolvableExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * Parse a template string with embedded {{...}} placeholders
 * into a ResolvableExpression tree.
 *
 * Responsibilities:
 * - Use Lexer to split TEXT vs EXPR tokens.
 * - For each EXPR token, detect variable vs function call.
 * - Build LiteralExpression, VariableExpression, FunctionExpression,
 *   and join them in CompositeExpression.
 *
 * This parser does not resolve anything.
 * It only builds the expression model.
 */
public class ExpressionParser {

    private final Lexer lexer = new Lexer();

    /**
     * Parse a full template string like:
     * "Hallo {{userName}}, Code: {{otp()}}"
     *
     * Return a CompositeExpression when multiple parts exist,
     * otherwise a single expression.
     */
    public ResolvableExpression parseTemplate(String template) {
        List<Token> tokens = lexer.tokenize(template);
        List<ResolvableExpression> parts = new ArrayList<ResolvableExpression>();

        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.getType() == Token.Type.TEXT) {
                parts.add(new LiteralExpression(t.getContent()));
            } else {
                parts.add(parseSingleExpr(t.getContent()));
            }
        }

        if (parts.size() == 1) {
            return parts.get(0);
        }
        return new CompositeExpression(parts);
    }

    /**
     * Parse the body of a single {{ ... }} block.
     * Supported:
     *   userName
     *   otp()
     *   wrap('x';{{userName}})
     *   wrap('x';{{wrap('y';{{userName}})}})
     */
    private ResolvableExpression parseSingleExpr(String body) {
        String trimmed = body.trim();

        // Detect function call: <name>(...)
        int parenIndex = indexOfTopLevelParen(trimmed);
        if (parenIndex > 0 && trimmed.endsWith(")")) {
            String functionName = trimmed.substring(0, parenIndex).trim();
            String argListRaw = trimmed.substring(parenIndex + 1, trimmed.length() - 1);
            List<ResolvableExpression> argExprs = parseFunctionArgs(argListRaw);
            return new FunctionExpression(functionName, argExprs);
        }

        // Fallback: treat as variable reference ({{foo}})
        return new VariableExpression(trimmed);
    }

    /**
     * Find the first '(' that is not inside quotes.
     * Return -1 if not found.
     */
    private int indexOfTopLevelParen(String text) {
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (inQuote) {
                if (c == quoteChar) {
                    inQuote = false;
                }
                continue;
            }

            if (c == '\'' || c == '"') {
                inQuote = true;
                quoteChar = c;
                continue;
            }

            if (c == '(') {
                return i;
            }
        }

        return -1;
    }

    /**
     * Parse "arg1;arg2;arg3" into separate ResolvableExpression objects.
     *
     * Rules:
     * - Split on ';' at top level.
     * - Respect quotes '...' and "..."
     * - Respect nested {{ ... }} blocks (track curlies depth).
     */
    private List<ResolvableExpression> parseFunctionArgs(String raw) {
        List<String> chunks = splitArgs(raw);
        List<ResolvableExpression> result = new ArrayList<ResolvableExpression>();
        for (int i = 0; i < chunks.size(); i++) {
            result.add(buildArgExpression(chunks.get(i).trim()));
        }
        return result;
    }

    /**
     * Build a ResolvableExpression from a single argument token.
     *
     * Supported forms:
     * - 'literal'
     * - "literal"
     * - {{userName}}
     * - {{otp()}}
     * - plainVarName
     */
    private ResolvableExpression buildArgExpression(String token) {
        if (token.startsWith("{{") && token.endsWith("}}")) {
            String inner = token.substring(2, token.length() - 2);
            return parseSingleExpr(inner);
        }

        if (isQuoted(token)) {
            return new LiteralExpression(unquote(token));
        }

        // bare variable name
        return new VariableExpression(token);
    }

    private boolean isQuoted(String s) {
        if (s == null || s.length() < 2) {
            return false;
        }
        char first = s.charAt(0);
        char last  = s.charAt(s.length() - 1);
        return (first == '\'' && last == '\'') ||
                (first == '"'  && last == '"');
    }

    private String unquote(String s) {
        if (isQuoted(s)) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /**
     * Split raw argument list "a; b; {{wrap('x';{{u}})}}" into logical chunks,
     * honoring quotes and nested {{...}}.
     */
    private List<String> splitArgs(String raw) {
        List<String> parts = new ArrayList<String>();
        StringBuilder current = new StringBuilder();

        boolean inQuote = false;
        char quoteChar = 0;

        int braceDepth = 0; // depth of nested {{...}}

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);

            // handle quote state first
            if (inQuote) {
                current.append(c);
                if (c == quoteChar) {
                    inQuote = false;
                }
                continue;
            }

            // not currently in a quote
            if (c == '\'' || c == '"') {
                inQuote = true;
                quoteChar = c;
                current.append(c);
                continue;
            }

            // track nested {{ ... }}
            if (c == '{' && i + 1 < raw.length() && raw.charAt(i + 1) == '{') {
                braceDepth++;
                current.append(c);
                i++;
                current.append(raw.charAt(i));
                continue;
            }
            if (c == '}' && i + 1 < raw.length() && raw.charAt(i + 1) == '}') {
                braceDepth--;
                current.append(c);
                i++;
                current.append(raw.charAt(i));
                continue;
            }

            // split on ';' only if not inside nested expression
            if (c == ';' && braceDepth == 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        // last chunk
        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }

        // edge case: empty arg list -> single empty string? No. Return empty list.
        if (parts.size() == 1 && parts.get(0).length() == 0) {
            if (raw.trim().length() == 0) {
                return new ArrayList<String>();
            }
        }

        return parts;
    }
}
