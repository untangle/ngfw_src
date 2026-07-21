/**
 * $Id$
 */
package com.untangle.uvm.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SQL utility methods for safe parsing and validation of user-supplied values.
 */
public class SqlUtil
{
    private static final Pattern NUMERIC_LITERAL = Pattern.compile("[+-]?\\d*\\.?\\d+");

    public static final int MAX_IN_VALUES = 1000;
    public static final int MAX_STRING_LITERAL_LENGTH = 1000;

    /**
     * The type of a parsed literal from an IN/NOT IN value list.
     */
    public static enum LiteralType { STRING, NUMBER }

    /**
     * A typed literal value parsed from an IN/NOT IN list.
     * Carries its type so PreparedStatement binding can use the correct
     * setXXX method without re-inferring the type from the string.
     */
    public static class InLiteral
    {
        public final LiteralType type;
        public final String stringValue;
        public final BigDecimal numberValue;

        /**
         * Creates an InLiteral instance.
         * @param type The type of the literal
         * @param stringValue The string value of the literal
         * @param numberValue The numeric value of the literal
         */
        private InLiteral( LiteralType type, String stringValue, BigDecimal numberValue )
        {
            this.type = type;
            this.stringValue = stringValue;
            this.numberValue = numberValue;
        }

        /**
         * Creates a STRING literal from a quoted value.
         * @param value the string content (without surrounding quotes)
         * @return a new InLiteral of type STRING
         */
        static InLiteral ofString( String value ) { return new InLiteral(LiteralType.STRING, value, null); }

        /**
         * Creates a NUMBER literal from a parsed numeric value.
         * @param value the numeric value
         * @return a new InLiteral of type NUMBER
         */
        static InLiteral ofNumber( BigDecimal value ) { return new InLiteral(LiteralType.NUMBER, null, value); }
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private SqlUtil() {}

    /**
     * Parse an IN/NOT IN value string into typed literal values.
     *
     * State-machine tokenizer that only accepts:
     *   - Single-quoted string literals (with '' escape for embedded quotes)
     *   - Numeric literals (integer or decimal, optionally signed, e.g. 123, +45, -3.14, .5)
     *
     * NULL is rejected inside IN lists because SQL IN/NOT IN with NULL
     * produces unintuitive results (NOT IN with NULL always returns zero rows).
     * Users should use IS NULL as a separate condition instead.
     *
     * After a quoted literal closes, the next non-whitespace character must be
     * a comma or end-of-input - trailing content like 'abc'xyz is rejected.
     *
     * Parentheses inside the value list are rejected (no nesting allowed).
     * The list is capped at {@value #MAX_IN_VALUES} values and each string
     * literal at {@value #MAX_STRING_LITERAL_LENGTH} characters.
     *
     * Everything else - expressions, function calls, subqueries, identifiers,
     * operators - is rejected by the whitelist validation.
     *
     * @param val the raw value string, e.g. "(1,2,3)" or "('A,B','C')"
     * @return list of typed literals; empty list if val is null or empty
     */
    public static List<InLiteral> parseInValues( String val )
    {
        if (val == null) return List.of();
        String trimmed = val.trim();

        if (trimmed.startsWith("(")) {
            if (!trimmed.endsWith(")")) {
                throw new SqlParseException("Unmatched opening parenthesis in IN value");
            }
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        } else if (trimmed.endsWith(")")) {
            throw new SqlParseException("Unmatched closing parenthesis in IN value");
        }
        if (trimmed.isEmpty()) return List.of();

        List<InLiteral> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        boolean wasQuoted = false;
        boolean quoteClosed = false;

        for (int pos = 0; pos < trimmed.length(); pos++) {
            char c = trimmed.charAt(pos);

            if (inQuote) {
                if (c == '\'') {
                    if (pos + 1 < trimmed.length() && trimmed.charAt(pos + 1) == '\'') {
                        current.append('\'');
                        pos++;
                    } else {
                        inQuote = false;
                        quoteClosed = true;
                    }
                } else {
                    current.append(c);
                    if (current.length() > MAX_STRING_LITERAL_LENGTH) {
                        throw new SqlParseException("String literal exceeds maximum length of " + MAX_STRING_LITERAL_LENGTH + " characters");
                    }
                }
            } else if (quoteClosed) {
                if (c == ',') {
                    addParsedLiteral(result, current.toString(), true);
                    current.setLength(0);
                    wasQuoted = false;
                    quoteClosed = false;
                } else if (Character.isWhitespace(c)) {
                    // whitespace after closing quote is ok
                } else {
                    throw new SqlParseException("Unexpected content after closing quote at position " + pos);
                }
            } else if (c == '\'') {
                if (current.toString().trim().length() > 0) {
                    throw new SqlParseException("Unexpected quote after content at position " + pos);
                }
                current.setLength(0);
                inQuote = true;
                wasQuoted = true;
            } else if (c == '(' || c == ')') {
                throw new SqlParseException("Parentheses not allowed inside IN value list at position " + pos);
            } else if (c == ',') {
                addParsedLiteral(result, current.toString(), false);
                current.setLength(0);
                wasQuoted = false;
            } else {
                current.append(c);
            }

            if (result.size() > MAX_IN_VALUES) {
                throw new SqlParseException("IN value list exceeds maximum of " + MAX_IN_VALUES + " values");
            }
        }

        if (inQuote) {
            throw new SqlParseException("Unterminated quote in IN value");
        }

        addParsedLiteral(result, current.toString(), wasQuoted || quoteClosed);

        if (result.size() > MAX_IN_VALUES) {
            throw new SqlParseException("IN value list exceeds maximum of " + MAX_IN_VALUES + " values");
        }

        return result;
    }

    /**
     * Validate a single parsed IN-list token and add it to the result list.
     * Quoted strings become STRING literals.
     * Unquoted tokens must be numeric literals - NULL and everything else
     * is rejected.
     *
     * @param result the list to add the validated literal to
     * @param raw the raw token string from the parser
     * @param wasQuoted true if the token was enclosed in single quotes
     */
    private static void addParsedLiteral( List<InLiteral> result, String raw, boolean wasQuoted )
    {
        String token = raw.trim();

        if (wasQuoted) {
            result.add(InLiteral.ofString(token));
            return;
        }

        if (token.isEmpty()) {
            throw new SqlParseException("Empty value between commas in IN list");
        }

        if (Constants.NULL.equalsIgnoreCase(token)) {
            throw new SqlParseException("NULL is not allowed in IN lists (use IS NULL condition instead)");
        }

        if (NUMERIC_LITERAL.matcher(token).matches()) {
            result.add(InLiteral.ofNumber(new BigDecimal(token)));
            return;
        }

        throw new SqlParseException("Unrecognized token '" + token + "' in IN list");
    }
}
