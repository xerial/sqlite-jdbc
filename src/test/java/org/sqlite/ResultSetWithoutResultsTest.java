package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

/** Tests to make sure we properly reset a query when there are no results */
public class ResultSetWithoutResultsTest {

    /**
     * Validates that Statements can be reused as expected even when the result set has no results
     *
     * @throws Exception
     */
    @Test
    public void testQueryIsReset() throws Exception {
        try (Connection con = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement statement = con.createStatement()) {
                // run a few queries with no results
                for (int i = 0; i < 3; i++) {
                    runEmptyStatement(statement);
                }
                // test a query that has a result.
                testStmtWithResult(statement);
            }
        }
    }

    private void testStmtWithResult(Statement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery("select 123")) {
            assertThat(rs.isBeforeFirst()).isTrue();
            assertThat(rs.isAfterLast()).isFalse();
            rs.next();
            assertThat(rs.isBeforeFirst()).isFalse();
            assertThat(rs.isAfterLast()).isFalse();
            assertThat(rs.getInt(1)).isEqualTo(123);
            rs.next();
            assertThat(rs.isAfterLast()).isTrue();
        }
    }

    private void runEmptyStatement(Statement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery("select 1 where 1=0")) {
            assertThat(rs.isBeforeFirst()).isFalse();
            assertThat(rs.isAfterLast()).isFalse();
        }
    }
}
