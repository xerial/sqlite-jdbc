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
package org.sqlite;

import java.sql.*;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Calendar;
import java.util.Map;

/** Implements a JDBC ResultSet.
 *
 * As only one ResultSet can exist per statement, this implementation
 * takes the odd step of making the ResultSet and Statement the same
 * object. This means:
 *     ResultSet rs = statement.executeQuery("SELECT ...");
 *
 * Generates no temporary ResultSet object, it just returns itself.
 * When a great many ResultSets are used (e.g. in a loop), this can
 * help reduce the load on the Garbage collector.
 *
 * As a result of this odd arrangement, Stmt and PrepStmt must
 * extend RS:
 *     Object -- Unused -- RS -- Stmt
 *                          | -- PrepStmt
 *
 * Such inheritance requires careful checking of the object state,
 * for which the check...() functions and isRS() function handle.
 */
abstract class RS extends Unused implements ResultSet, ResultSetMetaData, Codes
{
    Conn conn;
    DB db;

    String sql = null;
    long pointer = 0;
    boolean isAfterLast = false;
    boolean resultsWaiting = false;

    int maxRows;              // max. number of rows as set by a Statement
    String[] cols = null;     // if null, the RS is closed()
    String[] colsMeta = null; // same as cols, but used by Meta interface
    boolean[][] meta = null;

    private int limitRows; // 0 means no limit, must check against maxRows
    private int row = 1;   // number of current row, starts at 1
    private int lastCol;   // last column accessed, for wasNull(). -1 if none

    RS(Conn conn) {
        this.conn = conn;
        this.db = conn.db();
    }


    // INTERNAL FUNCTIONS ///////////////////////////////////////////

    protected final void checkOpen() throws SQLException {
        if (db == null) throw new SQLException("statement is closed");
    }
    protected final void checkExec() throws SQLException {
        if (pointer == 0) throw new SQLException("statement is not executing");
    }
    protected final void checkRS() throws SQLException {
        if (db == null || !isRS()) throw new SQLException("ResultSet closed");
    }
    /** Returns true if this Statement is an currently an active ResultSet. */
    protected final boolean isRS() { return cols != null; }

    // takes col in [1,x] form, returns in [0,x-1] form
    private int checkCol(int col) throws SQLException {
        checkOpen();
        if (colsMeta == null) throw new IllegalStateException(
            "SQLite JDBC: inconsistent internal state");
        if (col < 1 || col > colsMeta.length) throw new SQLException(
            "column " + col + " out of bounds [1," + colsMeta.length + "]");
        return --col;
    }

    // takes col in [1,x] form, marks it as last accessed and returns [0,x-1]
    private int markCol(int col) throws SQLException {
        checkRS(); checkCol(col); lastCol = col; return --col;
    }

    private void checkMeta() throws SQLException {
        checkCol(1);
        if (meta == null) meta = db.column_metadata(pointer);
    }


    // ResultSet Functions //////////////////////////////////////////

    // returns col in [1,x] form
    public int findColumn(String col) throws SQLException {
        checkRS();
        for (int i=0; i < cols.length; i++)
            if (col.equalsIgnoreCase(cols[i])) return i+1;
        throw new SQLException("no such column: '"+col+"'");
    }

    public boolean next() throws SQLException {
        if (isAfterLast) return false;  // finished ResultSet
        lastCol = -1;

        // first row is loaded by execute(), so do not step() again
        if (row == 1) { row++; return true; }

        // check if we are row limited by the statement or the ResultSet
        if (maxRows != 0 && row > maxRows) return false;
        if (limitRows != 0 && row >= limitRows) return false;

        // do the real work
        switch (db.step(pointer)) {
            case SQLITE_DONE:
                isAfterLast = true;
                close();      // agressive closing to avoid writer starvation
                return false;
            case SQLITE_ROW: row++; return true;
            case SQLITE_BUSY:
                throw new SQLException("database locked");
            default:
                 db.throwex(); return false;
        }
    }

    public int getType() throws SQLException { return TYPE_FORWARD_ONLY; }

    public int getFetchSize() throws SQLException { return limitRows; }
    public void setFetchSize(int rows) throws SQLException {
        if (0 > rows || (maxRows != 0 && rows > maxRows))
            throw new SQLException("fetch size " + rows
                                   + " out of bounds " + maxRows);
        limitRows = rows; 
    }

    public int getFetchDirection() throws SQLException {
        checkOpen(); return ResultSet.FETCH_FORWARD; }
    public void setFetchDirection(int d) throws SQLException {
        checkOpen();
        if (d != ResultSet.FETCH_FORWARD)
            throw new SQLException("only FETCH_FORWARD direction supported");
    }

    public boolean isAfterLast() throws SQLException { return isAfterLast; }
    public boolean isBeforeFirst() throws SQLException {
        return !isAfterLast && row == 1; }
    public boolean isFirst() throws SQLException { return row == 2; }
    public boolean isLast() throws SQLException { // FIXME
        throw new SQLException("function not yet implemented for SQLite"); }

    /** Resets the RS in a way safe for both Stmt and PrepStmt.
     *  Full reset happens in Stmt.close(). */
    void clearRS() throws SQLException {
        cols = null;
        isAfterLast = true;
        limitRows = 0;
        row = 1;
        lastCol = -1;
    }
    protected void finalize() throws SQLException { clearRS(); }

    public int getRow() throws SQLException { return row; }

    public boolean wasNull() throws SQLException {
        return db.column_type(pointer, markCol(lastCol)) == SQLITE_NULL;
    }


    // DATA ACCESS FUNCTIONS ////////////////////////////////////////

    public boolean getBoolean(int col) throws SQLException {
        return getInt(col) == 0 ? false : true; }
    public boolean getBoolean(String col) throws SQLException {
        return getBoolean(findColumn(col)); }

    public byte getByte(int col) throws SQLException {
        return (byte)getInt(col); }
    public byte getByte(String col) throws SQLException {
        return getByte(findColumn(col)); }

    public byte[] getBytes(int col) throws SQLException {
        return db.column_blob(pointer, markCol(col)); }
    public byte[] getBytes(String col) throws SQLException {
        return getBytes(findColumn(col)); }

    public Date getDate(int col) throws SQLException {
        return new Date(db.column_long(pointer, markCol(col))); }
    public Date getDate(int col, Calendar cal) throws SQLException {
        if (cal == null) return getDate(col);
        cal.setTimeInMillis(db.column_long(pointer, markCol(col)));
        return new Date(cal.getTime().getTime());
    }
    public Date getDate(String col) throws SQLException {
        return getDate(findColumn(col), Calendar.getInstance()); }
    public Date getDate(String col, Calendar cal) throws SQLException {
        return getDate(findColumn(col), cal); }

    public double getDouble(int col) throws SQLException {
        if (db.column_type(pointer, markCol(col)) == SQLITE_NULL)
            return 0;
        return db.column_double(pointer, markCol(col));
    }
    public double getDouble(String col) throws SQLException {
        return getDouble(findColumn(col)); }

    public float getFloat(int col) throws SQLException {
        if (db.column_type(pointer, markCol(col)) == SQLITE_NULL)
            return 0;
        return (float)db.column_double(pointer, markCol(col));
    }
    public float getFloat(String col) throws SQLException {
        return getFloat(findColumn(col)); }

    public int getInt(int col) throws SQLException {
        return db.column_int(pointer, markCol(col)); }
    public int getInt(String col) throws SQLException {
        return getInt(findColumn(col)); }

    public long getLong(int col) throws SQLException {
        return db.column_long(pointer, markCol(col)); }
    public long getLong(String col) throws SQLException {
        return getLong(findColumn(col)); }

    public short getShort(int col) throws SQLException {
        return (short)getInt(col); }
    public short getShort(String col) throws SQLException {
        return getShort(findColumn(col)); }

    public String getString(int col) throws SQLException {
        return db.column_text(pointer, markCol(col)); }
    public String getString(String col) throws SQLException {
        return getString(findColumn(col)); }

    public Time getTime(int col) throws SQLException {
        return new Time(db.column_long(pointer, markCol(col))); }
    public Time getTime(int col, Calendar cal) throws SQLException {
        if (cal == null) return getTime(col);
        cal.setTimeInMillis(db.column_long(pointer, markCol(col)));
        return new Time(cal.getTime().getTime());
    }
    public Time getTime(String col) throws SQLException {
        return getTime(findColumn(col)); }
    public Time getTime(String col, Calendar cal) throws SQLException {
        return getTime(findColumn(col), cal); }

    public Timestamp getTimestamp(int col) throws SQLException {
        return new Timestamp(db.column_long(pointer, markCol(col))); }
    public Timestamp getTimestamp(int col, Calendar cal) throws SQLException {
        if (cal == null) return getTimestamp(col);
        cal.setTimeInMillis(db.column_long(pointer, markCol(col)));
        return new Timestamp(cal.getTime().getTime());
    }
    public Timestamp getTimestamp(String col) throws SQLException {
        return getTimestamp(findColumn(col)); }
    public Timestamp getTimestamp(String c, Calendar ca) throws SQLException {
        return getTimestamp(findColumn(c), ca); }

    public Object getObject(int col) throws SQLException {
        switch (db.column_type(pointer, checkCol(col))) {
            case SQLITE_INTEGER:
                long val = getLong(col);
                if (val > (long)Integer.MAX_VALUE
                        || val < (long)Integer.MIN_VALUE)
                    return new Long(val);
                else
                    return new Integer((int)val);
            case SQLITE_FLOAT:   return new Double(getDouble(col));
            case SQLITE_BLOB:    return getBytes(col);
            case SQLITE_NULL:    return null;
            case SQLITE_TEXT:
            default:
                return getString(col);
        }
    }
    public Object getObject(String col) throws SQLException {
        return getObject(findColumn(col)); }


    // ResultSetMetaData Functions //////////////////////////////////

    // we do not need to check the RS is open, only that colsMeta
    // is not null, done with checkCol(int).

    public String getCatalogName(int col) throws SQLException {
        return db.column_table_name(pointer, checkCol(col)); }
    public String getColumnClassName(int col) throws SQLException {
        checkCol(col); return "java.lang.Object"; }
    public int getColumnCount() throws SQLException {
        checkCol(1); return colsMeta.length;
    }
    public int getColumnDisplaySize(int col) throws SQLException {
        return Integer.MAX_VALUE; }
    public String getColumnLabel(int col) throws SQLException {
        return getColumnName(col); }
    public String getColumnName(int col) throws SQLException {
        return db.column_name(pointer, checkCol(col)); }
    public int getColumnType(int col) throws SQLException {
        switch (db.column_type(pointer, checkCol(col))) {
            case SQLITE_INTEGER: return Types.INTEGER;
            case SQLITE_FLOAT:   return Types.FLOAT;
            case SQLITE_BLOB:    return Types.BLOB;
            case SQLITE_NULL:    return Types.NULL;
            case SQLITE_TEXT:
            default:
                return Types.VARCHAR;
        }
    }
    public String getColumnTypeName(int col) throws SQLException {
        return db.column_decltype(pointer, checkCol(col));
    }
    public int getPrecision(int col) throws SQLException { return 0; } // FIXME
    public int getScale(int col) throws SQLException { return 0; }
    public String getSchemaName(int col) throws SQLException { return ""; }
    public String getTableName(int col) throws SQLException {
        return db.column_table_name(pointer, checkCol(col)); }
    public int isNullable(int col) throws SQLException {
        checkMeta();
        return meta[checkCol(col)][1] ? columnNoNulls: columnNullable;
    }
    public boolean isAutoIncrement(int col) throws SQLException {
        checkMeta(); return meta[checkCol(col)][2]; }
    public boolean isCaseSensitive(int col) throws SQLException { return true; }
    public boolean isCurrency(int col) throws SQLException { return false; }
    public boolean isDefinitelyWritable(int col) throws SQLException {
        return true; } // FIXME: check db file constraints?
    public boolean isReadOnly(int col) throws SQLException { return false; }
    public boolean isSearchable(int col) throws SQLException { return true; }
    public boolean isSigned(int col) throws SQLException { return false; }
    public boolean isWritable(int col) throws SQLException { return true; }

    public int getConcurrency() throws SQLException { return CONCUR_READ_ONLY; }

    public boolean rowDeleted()  throws SQLException { return false; }
    public boolean rowInserted() throws SQLException { return false; }
    public boolean rowUpdated()  throws SQLException { return false; }

    public int getResultSetConcurrency() throws SQLException {
        checkOpen(); return ResultSet.CONCUR_READ_ONLY; }
    public int getResultSetHoldability() throws SQLException {
        checkOpen(); return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
    public int getResultSetType() throws SQLException {
        checkOpen(); return ResultSet.TYPE_FORWARD_ONLY; }


    // SHARED BY Stmt, PrepStmt /////////////////////////////////////

    public String getCursorName() throws SQLException { return null; }
    public void setCursorName(String name) {}

    public SQLWarning getWarnings() throws SQLException { return null; }
    public void clearWarnings() throws SQLException {}

    public Connection getConnection() throws SQLException {
        checkOpen(); return conn; }
    public ResultSetMetaData getMetaData() throws SQLException {
        checkOpen(); return this; }

    public void cancel() throws SQLException { checkExec(); db.interrupt(); }
    public int getQueryTimeout() throws SQLException {
        checkOpen(); return conn.getTimeout(); }
    public void setQueryTimeout(int seconds) throws SQLException {
        checkOpen();
        if (seconds < 0) throw new SQLException("query timeout must be >= 0");
        conn.setTimeout(1000 * seconds);
    }

    // TODO: write test
    public int getMaxRows() throws SQLException { checkOpen(); return maxRows; }
    public void setMaxRows(int max) throws SQLException {
        checkOpen();
        if (max < 0) throw new SQLException("max row count must be >= 0");
        maxRows = max;
    }

    public int getMaxFieldSize() throws SQLException { return 0; }
    public void setMaxFieldSize(int max) throws SQLException {
        if (max < 0) throw new SQLException(
            "max field size "+max+" cannot be negative");
    }

    public ResultSet getResultSet() throws SQLException {
        checkExec();
        if (isRS()) throw new SQLException("ResultSet already requested");
        if (db.column_count(pointer) == 0) throw new SQLException(
            "no ResultSet available");
        if (colsMeta == null) colsMeta = db.column_names(pointer);
        cols = colsMeta;

        isAfterLast = !resultsWaiting;
        if (resultsWaiting) resultsWaiting = false;
        return this;
    }

    /** As SQLite's last_insert_rowid() function is DB-specific not
     *  statement specific, this function introduces a race condition
     *  if the same connection is used by two threads and both insert. */
    public ResultSet getGeneratedKeys() throws SQLException {
        return ((MetaData)conn.getMetaData()).getGeneratedKeys();
    }

    /** SQLite does not support multiple results from execute(). */
    public boolean getMoreResults() throws SQLException {
        return getMoreResults(0);
    }
    public boolean getMoreResults(int c) throws SQLException {
        checkOpen();
        close(); // as we never have another result, clean up pointer
        return false;
    }
}
