package org.sqlite;

import java.sql.Connection;
import java.sql.SQLException;

/** Provides an interface for creating SQLite user-defined functions.
 *
 * <p>A subclass of <tt>org.sqlite.Profile</tt> can be registered with
 * <tt>Profile.setProfile()</tt>.</p>
 *
 * Eg.
 *
 * <pre>
 *      Class.forName("org.sqlite.JDBC");
 *      Connection conn = DriverManager.getConnection("jdbc:sqlite:");
 *
 *      Profile.setProfile(conn, new Profile() {
 *          @Override
 *          protected void xProfile(String sql, long duration) {
 *              System.out.println(sql);
 *          }
 *      });
 *
 *      conn.createStatement().execute("create table mytest(id, name);");
 *  </pre>
 *
 */
public abstract class Profile
{
    /**
     * Sets the given profile object as the SQLite profile function.
     *
     * @param conn The connection.
     * @param t The trace object to register.
     */
    public static final void setProfile(Connection conn, Profile p)
            throws SQLException {
        if (conn == null || !(conn instanceof SQLiteConnection)) {
            throw new SQLException("connection must be to an SQLite db");
        }
        if (conn.isClosed()) {
            throw new SQLException("connection closed");
        }

        SQLiteConnection sqlite = (SQLiteConnection)conn;
        sqlite.db().set_profile(p);
    }

    public static final void clearProfile(Connection conn)
    		 throws SQLException {
    	setProfile(conn, null);
    }

	/**
	 * xProfile is called at the completion of execution of an sql statement.
	 *
	 * @param sql
	 *            the statement executed
	 * @param duration
	 *            approximate wall-clock time in nanoseconds to execute
	 *            statement
	 */
    protected abstract void xProfile(String sql, long duration);

}
