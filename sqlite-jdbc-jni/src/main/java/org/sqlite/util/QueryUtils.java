package org.sqlite.util;

import java.util.List;
import java.util.stream.Collectors;

public class QueryUtils {
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
