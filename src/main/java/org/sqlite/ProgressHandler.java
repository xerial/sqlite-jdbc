package org.sqlite;

import java.sql.Connection;
import java.sql.SQLException;

import org.sqlite.core.Codes;
import org.sqlite.core.DB;

public abstract class ProgressHandler
{
    static ProgressHandler registeredHandler;

    public static final void setHandler(Connection conn, int vmCalls, ProgressHandler progressHandler, Object ctx) throws SQLException {
        if (conn == null || !(conn instanceof SQLiteConnection)) {
            throw new SQLException("connection must be to an SQLite db");
        }
        if (conn.isClosed()) {
            throw new SQLException("connection closed");
        }
        SQLiteConnection liteConn = (SQLiteConnection) conn;
        liteConn.db().register_progress_handler(vmCalls, progressHandler, ctx);
        ProgressHandler.registeredHandler = progressHandler;
    }

    protected abstract int progress(Object ctx) throws SQLException;
}
