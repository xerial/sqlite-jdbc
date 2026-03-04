package org.sqlite;

import java.sql.Connection;
import java.sql.SQLException;

/** https://www.sqlite.org/c3ref/busy_handler.html */
public abstract class BusyHandler {

    /**
     * commit the busy handler for the connection.
     *
     * @param conn the SQLite connection
     * @param busyHandler the busyHandler
     * @throws SQLException
     */
    private static void commitHandler(Connection conn, BusyHandler busyHandler)
            throws SQLException {

        if (!(conn instanceof SQLiteConnection)) {
            throw new SQLException("connection must be to an SQLite db");
        }

        if (conn.isClosed()) {
            throw new SQLException("connection closed");
        }

        SQLiteConnection sqliteConnection = (SQLiteConnection) conn;
        sqliteConnection.getDatabase().busy_handler(busyHandler);
    }

    /**
     * Sets a busy handler for the connection.
     *
     * @param conn the SQLite connection
     * @param busyHandler the busyHandler
     * @throws SQLException
     */
    public static final void setHandler(Connection conn, BusyHandler busyHandler)
            throws SQLException {
        commitHandler(conn, busyHandler);
    }

    /**
     * Clears any busy handler registered with the connection.
     *
     * @param conn the SQLite connection
     * @throws SQLException
     */
    public static final void clearHandler(Connection conn) throws SQLException {
        commitHandler(conn, null);
    }

    /**
     * https://www.sqlite.org/c3ref/busy_handler.html
     *
     * @param nbPrevInvok number of times that the busy handler has been invoked previously for the
     *     same locking event
     * @throws SQLException
     * @return If the busy callback returns 0, then no additional attempts are made to access the
     *     database and SQLITE_BUSY is returned to the application. If the callback returns
     *     non-zero, then another attempt is made to access the database and the cycle repeats.
     */
    protected abstract int callback(int nbPrevInvok) throws SQLException;
}
