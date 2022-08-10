package org.sqlite;

import static org.assertj.core.api.Assumptions.assumeThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static List<String> getCompileOptions(Connection conn) throws SQLException {
        List<String> compileOptions = new ArrayList<>();
        try (Statement stat = conn.createStatement()) {
            try (ResultSet rs = stat.executeQuery("pragma compile_options")) {
                while (rs.next()) {
                    compileOptions.add(rs.getString(1));
                }
            }
        }
        return compileOptions;
    }

    public static void assumeJdbcExtensions(Connection conn) throws SQLException {
        assumeThat(getCompileOptions(conn))
                .as("SQLite has to be compiled with JDBC Extensions")
                .contains("JDBC_EXTENSIONS");
    }

    public static void assumeMathFunctions(Connection conn) throws SQLException {
        assumeThat(getCompileOptions(conn))
                .as("SQLite has to be compiled with SQLITE_ENABLE_MATH_FUNCTIONS")
                .contains("ENABLE_MATH_FUNCTIONS");
    }

    public static void assumeJdbcExtensionsOrMathFunctions(Connection conn) throws SQLException {
        List<String> compileOptions = getCompileOptions(conn);
        boolean expected =
                compileOptions.contains("JDBC_EXTENSIONS")
                        || compileOptions.contains("ENABLE_MATH_FUNCTIONS");
        assumeThat(expected)
                .as(
                        "SQLite has to be compiled with JDBC Extensions or SQLITE_ENABLE_MATH_FUNCTIONS")
                .isTrue();
    }

    public static void assumeJdbcExtensionsWithoutMathFunctions(Connection conn)
            throws SQLException {
        List<String> compileOptions = getCompileOptions(conn);
        boolean expected =
                compileOptions.contains("JDBC_EXTENSIONS")
                        && !compileOptions.contains("ENABLE_MATH_FUNCTIONS");
        assumeThat(expected)
                .as(
                        "SQLite has to be compiled with JDBC Extensions and without SQLITE_ENABLE_MATH_FUNCTIONS")
                .isTrue();
    }
}
