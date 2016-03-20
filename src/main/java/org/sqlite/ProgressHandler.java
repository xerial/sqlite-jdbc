package org.sqlite;

import java.sql.Connection;
import java.sql.SQLException;

import org.sqlite.core.Codes;
import org.sqlite.core.DB;

public abstract class ProgressHandler
{
    public static final void setHandler(Connection conn, int vmCalls, ProgressHandler progressHandler) throws SQLException {
        if (conn == null || !(conn instanceof SQLiteConnection)) {
            throw new SQLException("connection must be to an SQLite db");
        }
        if (conn.isClosed()) {
            throw new SQLException("connection closed");
        }
        SQLiteConnection liteConn = (SQLiteConnection) conn;
        liteConn.db().register_progress_handler(vmCalls, progressHandler);
    }

    public static final void clearHandler(Connection conn) throws SQLException {
        SQLiteConnection liteConn = (SQLiteConnection) conn;
        liteConn.db().clear_progress_handler();
    }

    protected abstract int progress() throws SQLException;
}
