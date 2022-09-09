package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.sqlite.core.DB;

public class ErrorMessageTest {
    @Test
    public void moved() throws SQLException, IOException {
        File from = File.createTempFile("error-message-test-moved-from", ".sqlite");
        from.deleteOnExit();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + from.getAbsolutePath());
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("create table sample(id, name)");
        stmt.executeUpdate("insert into sample values(1, 'foo')");

        File to = File.createTempFile("error-message-test-moved-from", ".sqlite");
        assumeThat(to.delete()).isTrue();
        assumeThat(from.renameTo(to)).isTrue();

        assertThatThrownBy(() -> stmt.executeUpdate("insert into sample values(2, 'bar')"))
                .isInstanceOf(SQLException.class)
                .hasMessageStartingWith("[SQLITE_READONLY_DBMOVED]");

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
        stmt.executeUpdate("insert into sample values(1, 'foo')");
        stmt.close();
        conn.close();

        assumeThat(file.setReadOnly()).isTrue();

        conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        stmt = conn.createStatement();
        Statement finalStmt = stmt;
        assertThatThrownBy(() -> finalStmt.executeUpdate("insert into sample values(2, 'bar')"))
                .isInstanceOf(SQLException.class)
                .hasMessageStartingWith("[SQLITE_READONLY]");
        stmt.close();
        conn.close();
    }

    @Test
    public void cantOpenDir() throws IOException {
        File dir = File.createTempFile("error-message-test-cant-open-dir", "");
        assumeThat(dir.delete()).isTrue();
        assumeThat(dir.mkdir()).isTrue();
        dir.deleteOnExit();

        assertThatThrownBy(
                        () -> DriverManager.getConnection("jdbc:sqlite:" + dir.getAbsolutePath()))
                .isInstanceOf(SQLException.class)
                .hasMessageStartingWith("[SQLITE_CANTOPEN");
    }

    @Test
    public void shouldUsePlainErrorCodeAsVendorCodeAndExtendedAsResultCode()
            throws SQLException, IOException {
        File from = File.createTempFile("error-message-test-plain-1", ".sqlite");
        from.deleteOnExit();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + from.getAbsolutePath());
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("create table sample(id, name)");
        stmt.executeUpdate("insert into sample values(1, 'foo')");

        File to = File.createTempFile("error-message-test-plain-2", ".sqlite");
        assumeThat(to.delete()).isTrue();
        assumeThat(from.renameTo(to)).isTrue();

        assertThatThrownBy(() -> stmt.executeUpdate("insert into sample values(2, 'bar')"))
                .isInstanceOfSatisfying(
                        SQLiteException.class,
                        (ex) -> {
                            assertThat(SQLiteErrorCode.getErrorCode(ex.getErrorCode()))
                                    .isEqualTo(SQLiteErrorCode.SQLITE_READONLY);
                            assertThat(ex.getResultCode())
                                    .isEqualTo(SQLiteErrorCode.SQLITE_READONLY_DBMOVED);
                        })
                .hasMessageStartingWith("[SQLITE_READONLY_DBMOVED]");

        stmt.close();
        conn.close();
    }

    @Test
    public void unknownErrorExceptionMessageShouldContainOriginalErrorCode() {
        int errorCode = 1234567890;
        String errorMessage = "fictitious code";

        SQLiteException exception = DB.newSQLException(errorCode, errorMessage);

        assertThat(exception.getMessage())
                .contains(Integer.toString(errorCode))
                .contains(errorMessage)
                .startsWith(SQLiteErrorCode.UNKNOWN_ERROR.toString());
    }
}
