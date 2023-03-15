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
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.core.DB;

@DisabledInNativeImage // assertj Assumptions do not work in native-image tests
public class ErrorMessageTest {
    @TempDir File tempDir;

    @Test
    public void moved() throws SQLException, IOException {
        File from = File.createTempFile("error-message-test-moved-from", ".sqlite", tempDir);

        try (Connection conn =
                        DriverManager.getConnection("jdbc:sqlite:" + from.getAbsolutePath());
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("create table sample(id, name)");
            stmt.executeUpdate("insert into sample values(1, 'foo')");

            File to = File.createTempFile("error-message-test-moved-from", ".sqlite", tempDir);
            assumeThat(to.delete()).isTrue();
            assumeThat(from.renameTo(to)).isTrue();

            assertThatThrownBy(() -> stmt.executeUpdate("insert into sample values(2, 'bar')"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageStartingWith("[SQLITE_READONLY_DBMOVED]");
        }
    }

    @Test
    public void writeProtected() throws SQLException, IOException {
        File file = File.createTempFile("error-message-test-write-protected", ".sqlite", tempDir);

        try (Connection conn =
                        DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("create table sample(id, name)");
            stmt.executeUpdate("insert into sample values(1, 'foo')");
        }

        assumeThat(file.setReadOnly()).isTrue();

        try (Connection conn =
                        DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
                Statement stmt = conn.createStatement()) {
            assertThatThrownBy(() -> stmt.executeUpdate("insert into sample values(2, 'bar')"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageStartingWith("[SQLITE_READONLY]");
        }
    }

    @Test
    public void cantOpenDir() throws IOException {
        File dir = File.createTempFile("error-message-test-cant-open-dir", "", tempDir);
        assumeThat(dir.delete()).isTrue();
        assumeThat(dir.mkdir()).isTrue();

        assertThatThrownBy(
                        () -> DriverManager.getConnection("jdbc:sqlite:" + dir.getAbsolutePath()))
                .isInstanceOf(SQLException.class)
                .hasMessageStartingWith("[SQLITE_CANTOPEN");
    }

    @Test
    public void shouldUsePlainErrorCodeAsVendorCodeAndExtendedAsResultCode()
            throws SQLException, IOException {
        File from = File.createTempFile("error-message-test-plain-1", ".sqlite", tempDir);

        try (Connection conn =
                        DriverManager.getConnection("jdbc:sqlite:" + from.getAbsolutePath());
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("create table sample(id, name)");
            stmt.executeUpdate("insert into sample values(1, 'foo')");

            File to = File.createTempFile("error-message-test-plain-2", ".sqlite", tempDir);
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
        }
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
