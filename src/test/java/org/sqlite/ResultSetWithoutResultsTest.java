package org.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            assertTrue(rs.isBeforeFirst());
            assertFalse(rs.isAfterLast());
            rs.next();
            assertFalse(rs.isBeforeFirst());
            assertFalse(rs.isAfterLast());
            assertEquals(123, rs.getInt(1));
            rs.next();
            assertTrue(rs.isAfterLast());
        }
    }

    private void runEmptyStatement(Statement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery("select 1 where 1=0")) {
            assertFalse(rs.isBeforeFirst());
            assertFalse(rs.isAfterLast());
        }
    }
}
