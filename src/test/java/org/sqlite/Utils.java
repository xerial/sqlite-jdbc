package org.sqlite;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
        assumeTrue(
                getCompileOptions(conn).contains("JDBC_EXTENSIONS"),
                "SQLite has to be compiled with JDBC Extensions");
    }

    public static void assumeJdbcExtensionsOrMathFunctions(Connection conn) throws SQLException {
        List<String> compileOptions = getCompileOptions(conn);
        boolean expected =
                compileOptions.contains("JDBC_EXTENSIONS")
                        || compileOptions.contains("ENABLE_MATH_FUNCTIONS");
        assumeTrue(
                expected,
                "SQLite has to be compiled with JDBC Extensions or SQLITE_ENABLE_MATH_FUNCTIONS");
    }
}
