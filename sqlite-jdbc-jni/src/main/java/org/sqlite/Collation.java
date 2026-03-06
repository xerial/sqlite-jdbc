/*
 * Copyright (c) 2021 Gauthier Roebroeck <gauthier.roebroeck@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import org.sqlite.core.Codes;
import org.sqlite.core.DB;

/**
 * Provides an interface for creating SQLite user-defined collations.
 *
 * <p>A subclass of <tt>org.sqlite.Collation</tt> can be registered with <tt>Collation.create()</tt>
 * and called by the name it was given. All collations must implement <tt>xCompare(String,
 * String)</tt>, which is called when SQLite compares two strings using the custom collation. Eg.
 *
 * <pre>
 *      Class.forName("org.sqlite.JDBC");
 *      Connection conn = DriverManager.getConnection("jdbc:sqlite:");
 *
 *      Collation.create(conn, "REVERSE", new Collation() {
 *          protected int xCompare(String str1, String str2) {
 *              return str1.compareTo(str2) * -1;
 *          }
 *      });
 *
 *      conn.createStatement().execute("select c1 from t order by c1 collate REVERSE;");
 *  </pre>
 */
public abstract class Collation {
    private SQLiteConnection conn;
    private DB db;

    /**
     * Registers a given collation with the connection.
     *
     * @param conn The connection.
     * @param name The name of the collation.
     * @param f The collation to register.
     */
    public static final void create(Connection conn, String name, Collation f) throws SQLException {
        if (conn == null || !(conn instanceof SQLiteConnection)) {
            throw new SQLException("connection must be to an SQLite db");
        }
        if (conn.isClosed()) {
            throw new SQLException("connection closed");
        }

        f.conn = (SQLiteConnection) conn;
        f.db = f.conn.getDatabase();

        if (f.db.create_collation(name, f) != Codes.SQLITE_OK) {
            throw new SQLException("error creating collation");
        }
    }

    /**
     * Removes a named collation from the given connection.
     *
     * @param conn The connection to remove the collation from.
     * @param name The name of the collation.
     * @throws SQLException
     */
    public static final void destroy(Connection conn, String name) throws SQLException {
        if (conn == null || !(conn instanceof SQLiteConnection)) {
            throw new SQLException("connection must be to an SQLite db");
        }
        ((SQLiteConnection) conn).getDatabase().destroy_collation(name);
    }

    /**
     * Called by SQLite as a custom collation to compare two strings.
     *
     * @param str1 the first string in the comparison
     * @param str2 the second string in the comparison
     * @return an integer that is negative, zero, or positive if the first string is less than,
     *     equal to, or greater than the second, respectively
     */
    protected abstract int xCompare(String str1, String str2);
}
