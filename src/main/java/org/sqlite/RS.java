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

import java.io.Reader;
import java.io.StringReader;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Implements a JDBC ResultSet.
 */
final class RS extends Unused implements ResultSet, ResultSetMetaData, Codes
{
	private final DateFormat df = new SimpleDateFormat(SQLiteConfig.getDateStringFormat());
    private final Stmt stmt;
    private final DB   db;
    private final long DATE_INT_MULTIPLIER = (SQLiteConfig.getDateIntMultiplier());

    boolean            open     = false; // true means have results and can iterate them
    int                maxRows;         // max. number of rows as set by a Statement
    String[]           cols     = null; // if null, the RS is closed()
    String[]           colsMeta = null; // same as cols, but used by Meta interface
    boolean[][]        meta     = null;

    private int        limitRows;       // 0 means no limit, must check against maxRows
    private int        row      = 0;    // number of current row, starts at 1 (0 is for before loading data)
    private int        lastCol;         // last column accessed, for wasNull(). -1 if none

    RS(Stmt stmt) {
        this.stmt = stmt;
        this.db = stmt.db;
    }

    // INTERNAL FUNCTIONS ///////////////////////////////////////////

    boolean isOpen() {
        return open;
    }

    /* Throws SQLException if ResultSet is not open. */
    void checkOpen() throws SQLException {
        if (!open)
            throw new SQLException("ResultSet closed");
    }

    // takes col in [1,x] form, returns in [0,x-1] form
    private int checkCol(int col) throws SQLException {
        if (colsMeta == null)
            throw new IllegalStateException("SQLite JDBC: inconsistent internal state");
        if (col < 1 || col > colsMeta.length)
            throw new SQLException("column " + col + " out of bounds [1," + colsMeta.length + "]");
        return --col;
    }

    // takes col in [1,x] form, marks it as last accessed and returns [0,x-1]
    private int markCol(int col) throws SQLException {
        checkOpen();
        checkCol(col);
        lastCol = col;
        return --col;
    }

    private void checkMeta() throws SQLException {
        checkCol(1);
        if (meta == null)
            meta = db.column_metadata(stmt.pointer);
    }

    // ResultSet Functions //////////////////////////////////////////

    public void close() throws SQLException {
        cols = null;
        colsMeta = null;
        meta = null;
        open = false;
        limitRows = 0;
        row = 0;
        lastCol = -1;

        if (stmt == null)
            return;
        if (stmt != null && stmt.pointer != 0)
            db.reset(stmt.pointer);
    }

    // returns col in [1,x] form
    public int findColumn(String col) throws SQLException {
        checkOpen();
        int c = -1;
        for (int i = 0; i < cols.length; i++) {
            if (col.equalsIgnoreCase(cols[i])
                    || (cols[i].toUpperCase().endsWith(col.toUpperCase()) && cols[i].charAt(cols[i].length()
                            - col.length()) == '.')) {
                if (c == -1)
                    c = i;
                else
                    throw new SQLException("ambiguous column: '" + col + "'");
            }
        }
        if (c == -1)
            throw new SQLException("no such column: '" + col + "'");
        else
            return c + 1;
    }

    public boolean next() throws SQLException {
        if (!open)
            return false; // finished ResultSet
        lastCol = -1;

        // first row is loaded by execute(), so do not step() again
        if (row == 0) {
            row++;
            return true;
        }

        // check if we are row limited by the statement or the ResultSet
        if (maxRows != 0 && row > maxRows)
            return false;

        // do the real work
        int statusCode = db.step(stmt.pointer);
        switch (statusCode) {
        case SQLITE_DONE:
            close(); // agressive closing to avoid writer starvation
            return false;
        case SQLITE_ROW:
            row++;
            return true;
        case SQLITE_BUSY:
        default:
            db.throwex(statusCode);
            return false;
        }
    }

    public int getType() throws SQLException {
        return TYPE_FORWARD_ONLY;
    }

    public int getFetchSize() throws SQLException {
        return limitRows;
    }

    public void setFetchSize(int rows) throws SQLException {
        if (0 > rows || (maxRows != 0 && rows > maxRows))
            throw new SQLException("fetch size " + rows + " out of bounds " + maxRows);
        limitRows = rows;
    }

    public int getFetchDirection() throws SQLException {
        checkOpen();
        return ResultSet.FETCH_FORWARD;
    }

    public void setFetchDirection(int d) throws SQLException {
        checkOpen();
        if (d != ResultSet.FETCH_FORWARD)
            throw new SQLException("only FETCH_FORWARD direction supported");
    }

    public boolean isAfterLast() throws SQLException {
        return !open;
    }

    public boolean isBeforeFirst() throws SQLException {
        return open && row == 0;
    }

    public boolean isFirst() throws SQLException {
        return row == 1;
    }

    public boolean isLast() throws SQLException { // FIXME
        throw new SQLException("function not yet implemented for SQLite");
    }

    @Override
    protected void finalize() throws SQLException {
        close();
    }

    public int getRow() throws SQLException {
        return row;
    }

    public boolean wasNull() throws SQLException {
        return db.column_type(stmt.pointer, markCol(lastCol)) == SQLITE_NULL;
    }

    // DATA ACCESS FUNCTIONS ////////////////////////////////////////

    public boolean getBoolean(int col) throws SQLException {
        return getInt(col) == 0 ? false : true;
    }

    public boolean getBoolean(String col) throws SQLException {
        return getBoolean(findColumn(col));
    }

    public byte getByte(int col) throws SQLException {
        return (byte) getInt(col);
    }

    public byte getByte(String col) throws SQLException {
        return getByte(findColumn(col));
    }

    public byte[] getBytes(int col) throws SQLException {
        return db.column_blob(stmt.pointer, markCol(col));
    }

    public byte[] getBytes(String col) throws SQLException {
        return getBytes(findColumn(col));
    }

    public Reader getCharacterStream(int col) throws SQLException {
        return new StringReader(getString(col));
    }

    public Reader getCharacterStream(String col) throws SQLException {
        return getCharacterStream(findColumn(col));
    }

    public Date getDate(int col) throws SQLException {
    	int data_type = db.column_type(stmt.pointer, markCol(col));
        if (data_type == SQLITE_NULL)
            return null;
        if (data_type == SQLITE_INTEGER) {
        	return new Date(db.column_long(stmt.pointer, markCol(col)) * DATE_INT_MULTIPLIER);
        }
        if (data_type == SQLITE_TEXT) {
			try {
				return new java.sql.Date(df.parse(db.column_text(stmt.pointer, markCol(col))).getTime());
			} catch (Exception e) {
				return null;
			}
        }
        if (data_type == SQLITE_FLOAT) {
        	return new Date(julianDateToCalendar(db.column_double(stmt.pointer, markCol(col))).getTimeInMillis());
        }
        return null;
    }

    public Date getDate(int col, Calendar cal) throws SQLException {
    	int data_type = db.column_type(stmt.pointer, markCol(col));
        if (data_type == SQLITE_NULL)
            return null;
        if (cal == null)
            return getDate(col);
        if (data_type == SQLITE_INTEGER) {
	        cal.setTimeInMillis(db.column_long(stmt.pointer, markCol(col)) * DATE_INT_MULTIPLIER);
	        return new Date(cal.getTime().getTime());
        }
        if (data_type == SQLITE_TEXT) {
			try {
				DateFormat df2 = (DateFormat) df.clone();
				df2.setCalendar(cal);
				return new java.sql.Date(df2.parse(db.column_text(stmt.pointer, markCol(col))).getTime());
			} catch (Exception e) {
				return null;
			}
        }
        if (data_type == SQLITE_FLOAT) {
        	return new Date(julianDateToCalendar(db.column_double(stmt.pointer, markCol(col)), cal).getTimeInMillis());
        }
        return null;
    }

    public Date getDate(String col) throws SQLException {
        return getDate(findColumn(col), Calendar.getInstance());
    }

    public Date getDate(String col, Calendar cal) throws SQLException {
        return getDate(findColumn(col), cal);
    }

    public double getDouble(int col) throws SQLException {
        if (db.column_type(stmt.pointer, markCol(col)) == SQLITE_NULL)
            return 0;
        return db.column_double(stmt.pointer, markCol(col));
    }

    public double getDouble(String col) throws SQLException {
        return getDouble(findColumn(col));
    }

    public float getFloat(int col) throws SQLException {
        if (db.column_type(stmt.pointer, markCol(col)) == SQLITE_NULL)
            return 0;
        return (float) db.column_double(stmt.pointer, markCol(col));
    }

    public float getFloat(String col) throws SQLException {
        return getFloat(findColumn(col));
    }

    public int getInt(int col) throws SQLException {
        return db.column_int(stmt.pointer, markCol(col));
    }

    public int getInt(String col) throws SQLException {
        return getInt(findColumn(col));
    }

    public long getLong(int col) throws SQLException {
        return db.column_long(stmt.pointer, markCol(col));
    }

    public long getLong(String col) throws SQLException {
        return getLong(findColumn(col));
    }

    public short getShort(int col) throws SQLException {
        return (short) getInt(col);
    }

    public short getShort(String col) throws SQLException {
        return getShort(findColumn(col));
    }

    public String getString(int col) throws SQLException {
        return db.column_text(stmt.pointer, markCol(col));
    }

    public String getString(String col) throws SQLException {
        return getString(findColumn(col));
    }

    public Time getTime(int col) throws SQLException {
        int data_type = db.column_type(stmt.pointer, markCol(col));
        if (data_type == SQLITE_NULL)
            return null;
        if (data_type == SQLITE_INTEGER) {
        	return new Time(db.column_long(stmt.pointer, markCol(col)) * DATE_INT_MULTIPLIER);
        }
        if (data_type == SQLITE_TEXT) {
			try {
				return new Time(df.parse(db.column_text(stmt.pointer, markCol(col))).getTime());
			} catch (Exception e) {
				return null;
			}
        }
        if (data_type == SQLITE_FLOAT) {
        	return new Time(julianDateToCalendar(db.column_double(stmt.pointer, markCol(col))).getTimeInMillis());
        }
        return null;
    }

    public Time getTime(int col, Calendar cal) throws SQLException {
        if (cal == null)
            return getTime(col);
        int data_type = db.column_type(stmt.pointer, markCol(col));
        if (data_type == SQLITE_NULL)
            return null;
        if (data_type == SQLITE_INTEGER) {
	        cal.setTimeInMillis(db.column_long(stmt.pointer, markCol(col)) * DATE_INT_MULTIPLIER);
	        return new Time(cal.getTime().getTime());
        }
        if (data_type == SQLITE_TEXT) {
			try {
				DateFormat df2 = (DateFormat) df.clone();
				df2.setCalendar(cal);
				return new Time(df2.parse(db.column_text(stmt.pointer, markCol(col))).getTime());
			} catch (Exception e) {
				return null;
			}
        }
        if (data_type == SQLITE_FLOAT) {
        	return new Time(julianDateToCalendar(db.column_double(stmt.pointer, markCol(col)), cal).getTimeInMillis());
        }
        return null;
    }

    public Time getTime(String col) throws SQLException {
        return getTime(findColumn(col));
    }

    public Time getTime(String col, Calendar cal) throws SQLException {
        return getTime(findColumn(col), cal);
    }

    public Timestamp getTimestamp(int col) throws SQLException {
        int data_type = db.column_type(stmt.pointer, markCol(col));
        if (data_type == SQLITE_NULL)
            return null;
        if (data_type == SQLITE_INTEGER) {
        	return new Timestamp(db.column_long(stmt.pointer, markCol(col)) * DATE_INT_MULTIPLIER);
        }
        if (data_type == SQLITE_TEXT) {
			try {
				return new Timestamp(df.parse(db.column_text(stmt.pointer, markCol(col))).getTime());
			} catch (Exception e) {
				return null;
			}
        }
        if (data_type == SQLITE_FLOAT) {
        	return new Timestamp(julianDateToCalendar(db.column_double(stmt.pointer, markCol(col))).getTimeInMillis());
        }
        return null;
    }

    public Timestamp getTimestamp(int col, Calendar cal) throws SQLException {
        if (cal == null)
            return getTimestamp(col);
        int data_type = db.column_type(stmt.pointer, markCol(col));
        if (data_type == SQLITE_NULL)
            return null;
        if (data_type == SQLITE_INTEGER) {
	        cal.setTimeInMillis(db.column_long(stmt.pointer, markCol(col)) * DATE_INT_MULTIPLIER);
	        return new Timestamp(cal.getTime().getTime());
        }
        if (data_type == SQLITE_TEXT) {
			try {
				DateFormat df2 = (DateFormat) df.clone();
				df2.setCalendar(cal);
				return new Timestamp(df2.parse(db.column_text(stmt.pointer, markCol(col))).getTime());
			} catch (Exception e) {
				return null;
			}
        }
        if (data_type == SQLITE_FLOAT) {
        	return new Timestamp(julianDateToCalendar(db.column_double(stmt.pointer, markCol(col)), cal).getTimeInMillis());
        }
        return null;
    }

    public Timestamp getTimestamp(String col) throws SQLException {
        return getTimestamp(findColumn(col));
    }

    public Timestamp getTimestamp(String c, Calendar ca) throws SQLException {
        return getTimestamp(findColumn(c), ca);
    }

    public Object getObject(int col) throws SQLException {
        switch (db.column_type(stmt.pointer, checkCol(col))) {
        case SQLITE_INTEGER:
            long val = getLong(col);
            if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE)
                return new Long(val);
            else
                return new Integer((int) val);
        case SQLITE_FLOAT:
            return new Double(getDouble(col));
        case SQLITE_BLOB:
            return getBytes(col);
        case SQLITE_NULL:
            return null;
        case SQLITE_TEXT:
        default:
            return getString(col);
        }
    }

    public Object getObject(String col) throws SQLException {
        return getObject(findColumn(col));
    }

    public Statement getStatement() {
        return stmt;
    }

    public String getCursorName() throws SQLException {
        return null;
    }

    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    public void clearWarnings() throws SQLException {}

    // ResultSetMetaData Functions //////////////////////////////////

    // we do not need to check the RS is open, only that colsMeta
    // is not null, done with checkCol(int).

    public ResultSetMetaData getMetaData() throws SQLException {
        return this;
    }

    public String getCatalogName(int col) throws SQLException {
        return db.column_table_name(stmt.pointer, checkCol(col));
    }

    public String getColumnClassName(int col) throws SQLException {
        checkCol(col);
        return "java.lang.Object";
    }

    public int getColumnCount() throws SQLException {
        checkCol(1);
        return colsMeta.length;
    }

    public int getColumnDisplaySize(int col) throws SQLException {
        return Integer.MAX_VALUE;
    }

    public String getColumnLabel(int col) throws SQLException {
        return getColumnName(col);
    }

    public String getColumnName(int col) throws SQLException {
        return db.column_name(stmt.pointer, checkCol(col));
    }

    public int getColumnType(int col) throws SQLException {
        switch (db.column_type(stmt.pointer, checkCol(col))) {
        case SQLITE_INTEGER:
            return Types.INTEGER;
        case SQLITE_FLOAT:
            return Types.FLOAT;
        case SQLITE_BLOB:
            return Types.BLOB;
        case SQLITE_NULL:
            return Types.NULL;
        case SQLITE_TEXT:
        default:
            return Types.VARCHAR;
        }
    }

    public String getColumnTypeName(int col) throws SQLException {
        switch (db.column_type(stmt.pointer, checkCol(col))) {
        case SQLITE_INTEGER:
            return "integer";
        case SQLITE_FLOAT:
            return "float";
        case SQLITE_BLOB:
            return "blob";
        case SQLITE_NULL:
            return "null";
        case SQLITE_TEXT:
        default:
            return "text";
        }
    }

    public int getPrecision(int col) throws SQLException {
        return 0;
    } // FIXME

    public int getScale(int col) throws SQLException {
        return 0;
    }

    public String getSchemaName(int col) throws SQLException {
        return "";
    }

    public String getTableName(int col) throws SQLException {
        return db.column_table_name(stmt.pointer, checkCol(col));
    }

    public int isNullable(int col) throws SQLException {
        checkMeta();
        return meta[checkCol(col)][1] ? columnNoNulls : columnNullable;
    }

    public boolean isAutoIncrement(int col) throws SQLException {
        checkMeta();
        return meta[checkCol(col)][2];
    }

    public boolean isCaseSensitive(int col) throws SQLException {
        return true;
    }

    public boolean isCurrency(int col) throws SQLException {
        return false;
    }

    public boolean isDefinitelyWritable(int col) throws SQLException {
        return true;
    } // FIXME: check db file constraints?

    public boolean isReadOnly(int col) throws SQLException {
        return false;
    }

    public boolean isSearchable(int col) throws SQLException {
        return true;
    }

    public boolean isSigned(int col) throws SQLException {
        return false;
    }

    public boolean isWritable(int col) throws SQLException {
        return true;
    }

    public int getConcurrency() throws SQLException {
        return CONCUR_READ_ONLY;
    }

    public boolean rowDeleted() throws SQLException {
        return false;
    }

    public boolean rowInserted() throws SQLException {
        return false;
    }

    public boolean rowUpdated() throws SQLException {
        return false;
    }
    /**
     * Added julianDateToCalendar functions for transforming a Julian Date to java.util.Calendar object.
     */
    private Calendar julianDateToCalendar(Double jd) {
    	if (jd == null)
    		return null;
    	return julianDateToCalendar(jd, Calendar.getInstance());    	
    }

    /**
     * Added julianDateToCalendar functions for transforming a Julian Date to java.util.Calendar object.
     * Based on Guine Christian's function found here: http://java.ittoolbox.com/groups/technical-functional/java-l/java-function-to-convert-julian-date-to-calendar-date-1947446
     */
	private Calendar julianDateToCalendar(Double jd, Calendar cal) {
		if (jd == null)
			return null;
		int yyyy = 0;
		int mm = 0;
		int dd = 0;
		int hh = 0;
		int mn = 0;
		int ss = 0;
		int ms = 0;
		int A = 0;
		double w = jd + 0.5;
		int Z = (int) w;
		double F = w - Z;
		if (Z < 2299161) {
			A = Z;
		} else {
			int alpha = (int) ((Z - 1867216.25) / 36524.25);
			A = Z + 1 + alpha - (int) (alpha / 4.0);
		}
		int B = A + 1524;
		int C = (int) ((B - 122.1) / 365.25);
		int D = (int) (365.25 * C);
		int E = (int) ((B - D) / 30.6001);
		// Day of month in decimals
		double jjd = B - D - (int) (30.6001 * E) + F;
		dd = (int) jjd;
		// Calculate hour
		double hhd = jjd - dd;
		hh = (int) (24 * hhd);
		// Calculate minutes
		double mnd = (24 * hhd) - hh;
		mn = (int) (60 * mnd);
		// Calculate seconds
		double ssd = (60 * mnd) - mn;
		ss = (int) (60 * ssd);
		// Calculate milliseconds
		double msd = (60 * ssd) - ss;
		ms = (int) (1000 * msd);
		// Calculate month
		if (E < 13.5) {
			mm = E - 1;
		} else {
			mm = E - 13;
		}
		// Calculate year
		if (mm > 2.5) {
			yyyy = C - 4716;
		} else {
			yyyy = C - 4715;
		}
		cal.set(yyyy, mm-1, dd, hh, mn, ss);
		cal.set(Calendar.MILLISECOND, ms);
		if (yyyy<1) {
			cal.set(Calendar.ERA, GregorianCalendar.BC);
			cal.set(Calendar.YEAR, -(yyyy-1));
		}
		return cal;
	}
}
