package org.sqlite.util;

import java.util.List;
import java.util.stream.Collectors;

public class QueryUtils {
    /**
     * Escapes a value for use between the given SQL quote characters, by doubling every occurrence
     * of the quote character.
     *
     * @param val the value to escape, may be null
     * @param quote the quote character the value will be wrapped in, {@code '} for a string literal
     *     or {@code "} for an identifier
     * @return the escaped value, or null if {@code val} is null
     */
    public static String escape(final String val, final char quote) {
        if (val == null) {
            return null;
        }
        int len = val.length();
        StringBuilder buf = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            if (val.charAt(i) == quote) {
                buf.append(quote);
            }
            buf.append(val.charAt(i));
        }
        return buf.toString();
    }

    /**
     * Quotes a value as a SQLite identifier, so that it is always read as a single identifier.
     *
     * @param val the identifier to quote
     * @return the double quoted identifier
     */
    public static String quoteIdentifier(final String val) {
        return '"' + escape(val, '"') + '"';
    }

    /**
     * Build a SQLite query using the VALUES clause to return arbitrary values.
     *
     * @param columns list of column names
     * @param valuesList values to return as rows
     * @return SQL query as string
     */
    public static String valuesQuery(List<String> columns, List<List<Object>> valuesList) {
        valuesList.forEach(
                (list) -> {
                    if (list.size() != columns.size())
                        throw new IllegalArgumentException(
                                "values and columns must have the same size");
                });
        return "with cte("
                + String.join(",", columns)
                + ") as (values "
                + valuesList.stream()
                        .map(
                                (values) ->
                                        "("
                                                + values.stream()
                                                        .map(
                                                                (o -> {
                                                                    if (o instanceof String)
                                                                        return "'" + o + "'";
                                                                    if (o == null) return "null";
                                                                    return o.toString();
                                                                }))
                                                        .collect(Collectors.joining(","))
                                                + ")")
                        .collect(Collectors.joining(","))
                + ") select * from cte";
    }
}
