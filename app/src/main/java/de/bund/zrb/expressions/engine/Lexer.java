package de.bund.zrb.expressions.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Split an input string into TEXT and EXPR tokens.
 *
 * Rules:
 * - Find balanced {{ ... }} blocks.
 * - Allow nested {{ ... {{ ... }} ... }} using a depth counter.
 * - Everything outside becomes TEXT tokens.
 *
 * SRP:
 * - Only locate and slice tokens.
 * - Do not interpret semantics (variable vs function).
 */
public class Lexer {

    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<Token>();
        int position = 0;

        while (position < input.length()) {
            int start = input.indexOf("{{", position);

            // no more expressions → remaining tail is TEXT
            if (start < 0) {
                String tail = input.substring(position);
                if (tail.length() > 0) {
                    tokens.add(new Token(Token.Type.TEXT, tail));
                }
                break;
            }

            // add preceding TEXT part
            if (start > position) {
                String pre = input.substring(position, start);
                if (pre.length() > 0) {
                    tokens.add(new Token(Token.Type.TEXT, pre));
                }
            }

            // find matching "}}"
            int end = findMatchingEnd(input, start);
            if (end < 0) {
                throw new IllegalArgumentException("Unmatched '{{' at index " + start);
            }

            // body inside {{ ... }}
            String body = input.substring(start + 2, end).trim();
            tokens.add(new Token(Token.Type.EXPR, body));

            // continue
            position = end + 2;
        }

        return tokens;
    }

    /**
     * Find the matching "}}" for the sequence starting at 'start' where input[start..start+1] == "{{".
     * Use a depth counter to allow nested braces.
     */
    private int findMatchingEnd(String input, int start) {
        int depth = 0;

        for (int i = start; i < input.length() - 1; i++) {
            if (input.startsWith("{{", i)) {
                depth++;
                i++; // skip second '{'
                continue;
            }

            if (input.startsWith("}}", i)) {
                depth--;
                if (depth == 0) {
                    return i;
                }
                i++; // skip second '}'
            }
        }

        return -1; // not found
    }
}
