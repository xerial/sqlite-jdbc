package org.sqlite;

import java.sql.Connection;
import java.sql.SQLException;

/** Provides an interface for creating SQLite user-defined functions.
 *
 * <p>A subclass of <tt>org.sqlite.Trace</tt> can be registered with
 * <tt>Trace.setTrace()</tt>.</p>
 *
 * Eg.
 *
 * <pre>
 *      Class.forName("org.sqlite.JDBC");
 *      Connection conn = DriverManager.getConnection("jdbc:sqlite:");
 *
 *      Trace.register(conn, new Trace() {
 *          @Override
 *          protected void xTrace(String sql) {
 *              System.out.println(sql);
 *          }
 *      });
 *
 *      conn.createStatement().execute("pragma journal_mode;").close();
 *  </pre>
 *
 */
public abstract class Trace
{
    /**
     * Sets the given trace object as the SQLite trace function.
     *
     * @param conn The connection.
     * @param t The trace object to register.
     */
    public static final void setTrace(Connection conn, Trace t)
            throws SQLException {
        if (conn == null || !(conn instanceof SQLiteConnection)) {
            throw new SQLException("connection must be to an SQLite db");
        }
        if (conn.isClosed()) {
            throw new SQLException("connection closed");
        }

        SQLiteConnection sqlite = (SQLiteConnection)conn;
        sqlite.db().set_trace(t);
    }

    public static final void clearTrace(Connection conn)
    		 throws SQLException {
    	setTrace(conn, null);
    }

	/**
	 * xTrace is called at various times when an SQL statement is being run by
	 * DB.step().
	 *
	 * @param sql
	 *            the statement executed
	 * @param duration
	 *            approximate wall-clock time in nanoseconds to execute
	 *            statement
	 */
    protected abstract void xTrace(String sql);

}
