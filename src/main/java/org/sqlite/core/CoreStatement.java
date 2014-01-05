/*
 * Copyright (c) 2007 David Crawshaw <david@zentus.com>
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
package org.sqlite.core;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sqlite.SQLiteConnection;
import org.sqlite.jdbc4.JDBC4ResultSet;

public abstract class CoreStatement implements Codes
{
    public final SQLiteConnection conn;
    protected final DB   db;
    protected final CoreResultSet   rs;

    protected CoreDatabaseMetaData metadata;

    public long       pointer;
    protected String     sql            = null;

    protected int        batchPos;
    protected Object[]   batch          = null;
    protected boolean    resultsWaiting = false;

    protected CoreStatement(SQLiteConnection c) {
        conn = c;
        db = conn.db();
        rs = new JDBC4ResultSet(this);
    }

    /**
     * @throws SQLException If the database is not opened.
     */
    protected final void checkOpen() throws SQLException {
        if (pointer == 0)
            throw new SQLException("statement is not executing");
    }

    /**
     * @return True if the database is opened; false otherwise.
     * @throws SQLException
     */
    boolean isOpen() throws SQLException {
        return (pointer != 0);
    }

    /**
     * Calls sqlite3_step() and sets up results. Expects a clean stmt.
     * @return True if the ResultSet has at least one row; false otherwise. 
     * @throws SQLException If the given SQL statement is null or no database is open.
     */
    protected boolean exec() throws SQLException {
        if (sql == null)
            throw new SQLException("SQLiteJDBC internal error: sql==null");
        if (rs.isOpen())
            throw new SQLException("SQLite JDBC internal error: rs.isOpen() on exec.");

        boolean rc = false;
        try {
            rc = db.execute(this, null);
        }
        finally {
            resultsWaiting = rc;
        }

        return db.column_count(pointer) != 0;
    }

    /**
     * Executes SQL statement and throws SQLExceptions if the given SQL
     * statement is null or no database is open.
     * @param sql SQL statement.
     * @return True if the ResultSet has at least one row; false otherwise. 
     * @throws SQLException If the given SQL statement is null or no database is open.
     */
    protected boolean exec(String sql) throws SQLException {
        if (sql == null)
            throw new SQLException("SQLiteJDBC internal error: sql==null");
        if (rs.isOpen())
            throw new SQLException("SQLite JDBC internal error: rs.isOpen() on exec.");

        boolean rc = false;
        try {
            rc = db.execute(sql);
        }
        finally {
            resultsWaiting = rc;
        }

        return db.column_count(pointer) != 0;
    }

    protected void internalClose() throws SQLException {
        if (db.conn.isClosed())
            throw DB.newSQLException(SQLITE_ERROR, "Connection is closed");

        if (pointer == 0)
            return;

        rs.close();
        batch = null;
        batchPos = 0;
        int resp = db.finalize(this);

        if (resp != SQLITE_OK && resp != SQLITE_MISUSE)
            db.throwex();
    }

    public abstract ResultSet executeQuery(String sql, boolean closeStmt) throws SQLException;
}
