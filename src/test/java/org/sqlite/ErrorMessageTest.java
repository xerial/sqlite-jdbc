package org.sqlite;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class ErrorMessageTest {
    static class VendorCodeMatcher extends BaseMatcher<Object> {
        final SQLiteErrorCode expected;

        VendorCodeMatcher(SQLiteErrorCode expected) {
            this.expected = expected;
        }

        public boolean matches(Object o) {
            if (!(o instanceof SQLException)) {
                return false;
            }
            SQLException e = (SQLException) o;
            SQLiteErrorCode ec = SQLiteErrorCode.getErrorCode(e.getErrorCode());
            return ec == expected;
        }

        public void describeTo(Description description) {
            description
                .appendText("SQLException with error code ")
                .appendText(expected.name())
                .appendText(" (")
                .appendValue(expected.code)
                .appendText(")");
        }
    }

    static class ResultCodeMatcher extends BaseMatcher<Object> {
        final SQLiteErrorCode expected;

        ResultCodeMatcher(SQLiteErrorCode expected) {
            this.expected = expected;
        }

        public boolean matches(Object o) {
            if (!(o instanceof SQLiteException)) {
                return false;
            }
            SQLiteException e = (SQLiteException) o;
            return e.getResultCode() == expected;
        }

        public void describeTo(Description description) {
            description
                .appendText("SQLiteException with error code ")
                .appendText(expected.name())
                .appendText(" (")
                .appendValue(expected.code)
                .appendText(")");
        }
    }

    @Test
    public void moved() throws SQLException, IOException {
        File from = File.createTempFile("error-message-test-moved-from", ".sqlite");
        from.deleteOnExit();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + from.getAbsolutePath());
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("create table sample(id, name)");
        stmt.executeUpdate("insert into sample values(1, \"foo\")");

        File to = File.createTempFile("error-message-test-moved-from", ".sqlite");
        assumeTrue(to.delete());
        assumeTrue(from.renameTo(to));

        Exception exception = assertThrows(SQLException.class, () -> stmt.executeUpdate("insert into sample values(2, \"bar\")"));
        assertTrue(exception.getMessage().contains("[SQLITE_READONLY_DBMOVED]"));

        stmt.close();
        conn.close();
    }

    @Test
    public void writeProtected() throws SQLException, IOException {
        File file = File.createTempFile("error-message-test-write-protected", ".sqlite");
        file.deleteOnExit();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("create table sample(id, name)");
        stmt.executeUpdate("insert into sample values(1, \"foo\")");
        stmt.close();
        conn.close();

        assumeTrue(file.setReadOnly());

        conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        stmt = conn.createStatement();
        Statement finalStmt = stmt;
        Exception exception = assertThrows(SQLException.class, () -> finalStmt.executeUpdate("insert into sample values(2, \"bar\")"));
        assertTrue(exception.getMessage().contains("[SQLITE_READONLY]"));
        stmt.close();
        conn.close();
    }

    @Test
    public void cantOpenDir() throws SQLException, IOException {
        File dir = File.createTempFile("error-message-test-cant-open-dir", "");
        assumeTrue(dir.delete());
        assumeTrue(dir.mkdir());
        dir.deleteOnExit();

        Exception exception = assertThrows(SQLException.class, () -> DriverManager.getConnection("jdbc:sqlite:" + dir.getAbsolutePath()));
        assertTrue(exception.getMessage().contains("[SQLITE_CANTOPEN]")
            || exception.getMessage().contains("[SQLITE_CANTOPEN_ISDIR]"));
    }

    @Test
    public void shouldUsePlainErrorCodeAsVendorCodeAndExtendedAsResultCode() throws SQLException, IOException {
        File from = File.createTempFile("error-message-test-plain-1", ".sqlite");
        from.deleteOnExit();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + from.getAbsolutePath());
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("create table sample(id, name)");
        stmt.executeUpdate("insert into sample values(1, \"foo\")");

        File to = File.createTempFile("error-message-test-plain-2", ".sqlite");
        assumeTrue(to.delete());
        assumeTrue(from.renameTo(to));

        Exception exception = assertThrows(SQLException.class, () -> stmt.executeUpdate("insert into sample values(2, \"bar\")"));
        assertTrue(exception.getMessage().contains("[SQLITE_READONLY_DBMOVED]"));
        assertThat(exception, new VendorCodeMatcher(SQLiteErrorCode.SQLITE_READONLY));
        assertThat(exception, new ResultCodeMatcher(SQLiteErrorCode.SQLITE_READONLY_DBMOVED));


        stmt.close();
        conn.close();
    }
}
