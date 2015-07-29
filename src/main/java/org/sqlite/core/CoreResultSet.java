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

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements a JDBC ResultSet.
 */
public abstract class CoreResultSet implements Codes
{
    protected final CoreStatement stmt;
    protected final DB   db;

    public boolean            open     = false; // true means have results and can iterate them
    public int                maxRows;         // max. number of rows as set by a Statement
    public String[]           cols     = null; // if null, the RS is closed()
    public String[]           colsMeta = null; // same as cols, but used by Meta interface
    protected boolean[][]        meta     = null;

    protected int        limitRows;       // 0 means no limit, must check against maxRows
    protected int        row      = 0;    // number of current row, starts at 1 (0 is for before loading data)
    protected int        lastCol;         // last column accessed, for wasNull(). -1 if none

    public boolean closeStmt;
    protected Map<String, Integer> columnNameToIndex = null;

    /**
     * Default constructor for a given statement.
     * @param stmt The statement.
     * @param closeStmt TODO
     */
    protected CoreResultSet(CoreStatement stmt) {
        this.stmt = stmt;
        this.db = stmt.db;
    }

    // INTERNAL FUNCTIONS ///////////////////////////////////////////

    /**
     * Checks the status of the result set.
     * @return True if has results and can iterate them; false otherwise.
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * @throws SQLException if ResultSet is not open.
     */
    protected void checkOpen() throws SQLException {
        if (!open) {
            throw new SQLException("ResultSet closed");
        }
    }

    /**
     * Takes col in [1,x] form, returns in [0,x-1] form
     * @param col
     * @return
     * @throws SQLException
     */
    public int checkCol(int col) throws SQLException {
        if (colsMeta == null) {
            throw new IllegalStateException("SQLite JDBC: inconsistent internal state");
        }
        if (col < 1 || col > colsMeta.length) {
            throw new SQLException("column " + col + " out of bounds [1," + colsMeta.length + "]");
        }
        return --col;
    }

    /**
     * Takes col in [1,x] form, marks it as last accessed and returns [0,x-1]
     * @param col
     * @return
     * @throws SQLException
     */
    protected int markCol(int col) throws SQLException {
        checkOpen();
        checkCol(col);
        lastCol = col;
        return --col;
    }

    /**
     * @throws SQLException
     */
    public void checkMeta() throws SQLException {
        checkCol(1);
        if (meta == null) {
            meta = db.column_metadata(stmt.pointer);
        }
    }

    public void close() throws SQLException {
        cols = null;
        colsMeta = null;
        meta = null;
        open = false;
        limitRows = 0;
        row = 0;
        lastCol = -1;
        columnNameToIndex = null;

        if (stmt == null) {
            return;
        }

        if (stmt != null && stmt.pointer != 0) {
            db.reset(stmt.pointer);

            if (closeStmt) {
                closeStmt = false; // break recursive call
                ((Statement)stmt).close();
            }
        }
    }

    protected Integer findColumnIndexInCache(String col) {
        if (columnNameToIndex == null) {
            return null;
        }
        return columnNameToIndex.get(col);
    }

    protected int addColumnIndexInCache(String col, int index) {
        if (columnNameToIndex == null) {
            columnNameToIndex = new HashMap<String, Integer>(cols.length);
        }
        columnNameToIndex.put(col, index);
        return index;
    }
}
