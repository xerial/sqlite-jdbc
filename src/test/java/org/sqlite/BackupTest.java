// --------------------------------------
// sqlite-jdbc Project
//
// BackupTest.java
// Since: Feb 18, 2009
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.core.DB;

public class BackupTest {
    @TempDir File tempDir;

    @Test
    public void backupAndRestore() throws SQLException, IOException {
        // create a memory database
        File tmpFile = File.createTempFile("backup-test", ".sqlite", tempDir);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:")) {
            // memory DB to file
            try (Statement stmt = conn.createStatement()) {
                createTableAndInsertRows(stmt);

                stmt.executeUpdate("backup to " + tmpFile.getAbsolutePath());
            }

            // open another memory database
            try (Connection conn2 = DriverManager.getConnection("jdbc:sqlite:")) {
                try (Statement stmt2 = conn2.createStatement()) {
                    stmt2.execute("restore from " + tmpFile.getAbsolutePath());
                    try (ResultSet rs = stmt2.executeQuery("select * from sample")) {
                        int count = 0;
                        while (rs.next()) {
                            count++;
                        }

                        assertThat(count).isEqualTo(2);
                    }
                }
            }
        }
    }

    private void createTableAndInsertRows(Statement stmt) throws SQLException {
        stmt.executeUpdate("create table sample(id, name)");
        stmt.executeUpdate("insert into sample values(1, \"leo\")");
        stmt.executeUpdate("insert into sample values(2, \"yui\")");
    }

    @Test
    void testFailedBackupAndRestore() throws Exception {
        File tmpFile = File.createTempFile("backup-test", ".sqlite", tempDir);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:")) {
            // memory DB to file
            try (Statement stmt = conn.createStatement()) {
                createTableAndInsertRows(stmt);
                // This fails because we cannot write to a file starting with "("
                assertThatCode(
                                () ->
                                        stmt.executeUpdate(
                                                "backup to ("
                                                        + tmpFile.getAbsolutePath()
                                                        + "doesnotexist.sqlite"))
                        .isOfAnyClassIn(SQLiteException.class);

                // This fails because we read from a file that does not exist, and we should not
                // create it
                assertThatCode(
                                () ->
                                        stmt.executeUpdate(
                                                "restore from "
                                                        + tmpFile.getAbsolutePath()
                                                        + "doesnotexist.sqlite"))
                        .isOfAnyClassIn(SQLiteException.class);
            }
        }
    }

    @Test
    void restoreIsRejectedOnReadOnlyConnection() throws Exception {
        File dbFile = File.createTempFile("backup-test-target", ".sqlite", tempDir);
        File srcFile = File.createTempFile("backup-test-source", ".sqlite", tempDir);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
                Statement stmt = conn.createStatement()) {
            createTableAndInsertRows(stmt);
        }
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + srcFile);
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("create table sample(id, name)");
        }

        SQLiteConfig config = new SQLiteConfig();
        config.setExplicitReadOnly(true);
        try (Connection conn =
                DriverManager.getConnection("jdbc:sqlite:" + dbFile, config.toProperties())) {
            conn.setAutoCommit(false);
            conn.setReadOnly(true);
            try (Statement stmt = conn.createStatement()) {
                assertThatThrownBy(() -> stmt.executeUpdate("restore from " + srcFile))
                        .isInstanceOf(SQLiteException.class)
                        .hasMessageContaining("SQLITE_READONLY");
                assertThatThrownBy(() -> stmt.execute("restore from " + srcFile))
                        .isInstanceOf(SQLiteException.class)
                        .hasMessageContaining("SQLITE_READONLY");
            }
            conn.commit();
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("select count(*) from sample")) {
            assertThat(rs.getInt(1)).isEqualTo(2);
        }
    }

    @Test
    public void memoryToDisk() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:");
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("create table sample(id integer primary key autoincrement, name)");
            for (int i = 0; i < 10000; i++) {
                stmt.executeUpdate("insert into sample(name) values(\"leo\")");
            }

            File tmpFile = File.createTempFile("backup-test2", ".sqlite", tempDir);
            stmt.executeUpdate("backup to " + tmpFile.getAbsolutePath());
        }
    }

    @Test
    void testProgress() throws Exception {
        File tmpFile = File.createTempFile("backup-test", ".sqlite", tempDir);

        try (SQLiteConnection conn = JDBC.createConnection("jdbc:sqlite:", new Properties())) {
            // memory DB to file
            try (Statement stmt = conn.createStatement()) {
                createTableAndInsertRows(stmt);
            }

            // check that JNI updates java with progress of the DB Backup.
            AtomicInteger remainingStore = new AtomicInteger(-1);
            AtomicInteger pageCountStore = new AtomicInteger(-1);
            DB.ProgressObserver progressObserver =
                    (remaining, pageCount) -> {
                        remainingStore.set(remaining);
                        pageCountStore.set(pageCount);
                    };

            int rc =
                    conn.getDatabase()
                            .backup("main", tmpFile.getAbsolutePath(), progressObserver, 1, 1, 1);
            assertThat(rc).isEqualTo(SQLiteErrorCode.SQLITE_OK.code);
            assertThat(remainingStore.get()).isEqualTo(0);
            assertThat(pageCountStore.get()).isGreaterThan(0);
        }
    }
}
