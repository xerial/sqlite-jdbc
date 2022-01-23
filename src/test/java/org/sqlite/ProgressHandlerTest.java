package org.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sqlite.core.DB;
import org.sqlite.core.NativeDBHelper;

public class ProgressHandlerTest {
    private Connection conn;
    private Statement stat;

    @BeforeEach
    public void connect() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
    }

    @AfterEach
    public void close() throws SQLException {
        stat.close();
        conn.close();
    }

    private void workWork() throws SQLException {
        // Generate some work for the sqlite vm
        stat.executeUpdate("drop table if exists foo;");
        stat.executeUpdate("create table foo (id integer);");
        stat.executeUpdate("insert into foo (id) values (1);");
        for (int i = 0; i < 100; i++) {
            stat.executeQuery("select * from foo");
        }
    }

    @Test
    public void basicProgressHandler() throws Exception {
        final int[] calls = {0};
        ProgressHandler.setHandler(
                conn,
                1,
                new ProgressHandler() {
                    @Override
                    protected int progress() throws SQLException {
                        calls[0]++;
                        return 0;
                    }
                });
        workWork();
        assertTrue(calls[0] > 0);
    }

    @Test
    public void testUnregister() throws Exception {
        final int[] calls = {0};
        ProgressHandler.setHandler(
                conn,
                1,
                new ProgressHandler() {
                    @Override
                    protected int progress() throws SQLException {
                        calls[0]++;
                        return 0;
                    }
                });
        workWork();
        assertTrue(calls[0] > 0);
        int totalCalls = calls[0];
        ProgressHandler.clearHandler(conn);
        workWork();
        assertEquals(totalCalls, calls[0]);
    }

    @Test
    public void testInterrupt() throws Exception {

        try {
            ProgressHandler.setHandler(
                    conn,
                    1,
                    new ProgressHandler() {
                        @Override
                        protected int progress() throws SQLException {
                            return 1;
                        }
                    });
            workWork();
        } catch (SQLException ex) {
            // Expected error
            return;
        }
        // Progress function throws, not reached
        fail();
    }

    /**
     * Tests that clear progress helper is implemented as expected. Ensures that memory is free'd
     * and free'd pointers are set to null (0)
     *
     * @throws Exception on test failure
     */
    @Test
    public void testClearProgressHelper() throws Exception {
        SQLiteConnection sqliteConnection = (SQLiteConnection) conn;
        final DB database = sqliteConnection.getDatabase();
        setDummyHandler();
        assertNotEquals(0, NativeDBHelper.getProgressHandler(database));
        ProgressHandler.clearHandler(conn);
        assertEquals(0, NativeDBHelper.getProgressHandler(database));
        ProgressHandler.clearHandler(conn);

        setDummyHandler();
        assertNotEquals(0, NativeDBHelper.getProgressHandler(database));
        ProgressHandler.setHandler(conn, 1, null);
        assertEquals(0, NativeDBHelper.getProgressHandler(database));
        ProgressHandler.setHandler(conn, 1, null);

        setDummyHandler();
        assertNotEquals(0, NativeDBHelper.getProgressHandler(database));
        conn.close();
        assertEquals(0, NativeDBHelper.getProgressHandler(database));
    }

    private void setDummyHandler() throws SQLException {
        ProgressHandler.setHandler(
                conn,
                1,
                new ProgressHandler() {
                    @Override
                    protected int progress() throws SQLException {
                        return 0;
                    }
                });
    }
}
