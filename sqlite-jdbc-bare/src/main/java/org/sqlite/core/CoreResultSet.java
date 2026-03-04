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
import org.sqlite.SQLiteConnectionConfig;

/** Implements a JDBC ResultSet. */
public abstract class CoreResultSet implements Codes {
    protected final CoreStatement stmt;

    /** If the result set does not have any rows. */
    public boolean emptyResultSet = false;
    /** If the result set is open. Doesn't mean it has results. */
    public boolean open = false;
    /** Maximum number of rows as set by a Statement */
    public long maxRows;
    /** if null, the RS is closed() */
    public String[] cols = null;
    /** same as cols, but used by Meta interface */
    public String[] colsMeta = null;

    protected boolean[][] meta = null;

    /** 0 means no limit, must check against maxRows */
    protected int limitRows;
    /** number of current row, starts at 1 (0 is for before loading data) */
    protected int row = 0;

    protected boolean pastLastRow = false;
    /** last column accessed, for wasNull(). -1 if none */
    protected int lastCol;

    public boolean closeStmt;
    protected Map<String, Integer> columnNameToIndex = null;

    /**
     * Default constructor for a given statement.
     *
     * @param stmt The statement.
     */
    protected CoreResultSet(CoreStatement stmt) {
        this.stmt = stmt;
    }

    // INTERNAL FUNCTIONS ///////////////////////////////////////////

    protected DB getDatabase() {
        return stmt.getDatabase();
    }

    protected SQLiteConnectionConfig getConnectionConfig() {
        return stmt.getConnectionConfig();
    }

    /**
     * Checks the status of the result set.
     *
     * @return True if has results and can iterate them; false otherwise.
     */
    public boolean isOpen() {
        return open;
    }

    /** @throws SQLException if ResultSet is not open. */
    protected void checkOpen() throws SQLException {
        if (!open) {
            throw new SQLException("ResultSet closed");
        }
    }

    /**
     * Takes col in [1,x] form, returns in [0,x-1] form
     *
     * @param col
     * @return
     * @throws SQLException
     */
    public int checkCol(int col) throws SQLException {
        if (colsMeta == null) {
            throw new SQLException("SQLite JDBC: inconsistent internal state");
        }
        if (col < 1 || col > colsMeta.length) {
            throw new SQLException("column " + col + " out of bounds [1," + colsMeta.length + "]");
        }
        return --col;
    }

    /**
     * Takes col in [1,x] form, marks it as last accessed and returns [0,x-1]
     *
     * @param col
     * @return
     * @throws SQLException
     */
    protected int markCol(int col) throws SQLException {
        checkCol(col);
        lastCol = col;
        return --col;
    }

    /** @throws SQLException */
    public void checkMeta() throws SQLException {
        checkCol(1);
        if (meta == null) {
            meta = stmt.pointer.safeRun(DB::column_metadata);
        }
    }

    public void close() throws SQLException {
        cols = null;
        colsMeta = null;
        meta = null;
        limitRows = 0;
        row = 0;
        pastLastRow = false;
        lastCol = -1;
        columnNameToIndex = null;
        emptyResultSet = false;

        if (stmt.pointer.isClosed() || (!open && !closeStmt)) {
            return;
        }

        DB db = stmt.getDatabase();
        synchronized (db) {
            if (!stmt.pointer.isClosed()) {
                stmt.pointer.safeRunInt(DB::reset);

                if (closeStmt) {
                    closeStmt = false; // break recursive call
                    ((Statement) stmt).close();
                }
            }
        }

        open = false;
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
