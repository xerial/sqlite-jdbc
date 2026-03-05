package org.sqlite;

import java.sql.Connection;
import java.sql.SQLException;

/** https://www.sqlite.org/c3ref/progress_handler.html */
public abstract class ProgressHandler {
    /**
     * Sets a progress handler for the connection.
     *
     * @param conn the SQLite connection
     * @param vmCalls the approximate number of virtual machine instructions that are evaluated
     *     between successive invocations of the progressHandler
     * @param progressHandler the progressHandler
     * @throws SQLException
     */
    public static final void setHandler(
            Connection conn, int vmCalls, ProgressHandler progressHandler) throws SQLException {
        if (!(conn instanceof SQLiteConnection)) {
            throw new SQLException("connection must be to an SQLite db");
        }
        if (conn.isClosed()) {
            throw new SQLException("connection closed");
        }
        SQLiteConnection sqliteConnection = (SQLiteConnection) conn;
        sqliteConnection.getDatabase().register_progress_handler(vmCalls, progressHandler);
    }

    /**
     * Clears any progress handler registered with the connection.
     *
     * @param conn the SQLite connection
     * @throws SQLException
     */
    public static final void clearHandler(Connection conn) throws SQLException {
        SQLiteConnection sqliteConnection = (SQLiteConnection) conn;
        sqliteConnection.getDatabase().clear_progress_handler();
    }

    protected abstract int progress() throws SQLException;
}
