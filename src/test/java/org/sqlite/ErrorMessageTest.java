package org.sqlite;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class ErrorMessageTest {

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
}
