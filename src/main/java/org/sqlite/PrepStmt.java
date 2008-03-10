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
import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;

/** See comment in RS.java to explain the strange inheritance hierarchy. */
final class PrepStmt extends RS
        implements PreparedStatement, ParameterMetaData, Codes
{
    private int columnCount;
    private int paramCount;
    private int batchPos;
    private Object[] batch;

    PrepStmt(Conn conn, String sql) throws SQLException {
        super(conn);

        this.sql = sql;
        db.prepare(this);
        colsMeta = db.column_names(pointer);
        columnCount = db.column_count(pointer);
        paramCount = db.bind_parameter_count(pointer);
        batch = new Object[paramCount];
        batchPos = 0;
    }

    /** Weaker close to support object overriding (see docs in RS.java). */
    public void close() throws SQLException {
        batch = null;
        if (pointer == 0 || db == null) clearRS(); else clearParameters();
    }

    public void clearParameters() throws SQLException {
        checkOpen();
        clearRS();
        db.reset(pointer);
        batchPos = 0;
        if (batch != null)
            for (int i=0; i < batch.length; i++)
                batch[i] = null;
    }

    protected void finalize() throws SQLException {
        db.finalize(this);
        // TODO
    }


    public boolean execute() throws SQLException {
        checkExec();
        clearRS();
        db.reset(pointer); // TODO: needed?
        resultsWaiting = db.execute(this, batch);
        return columnCount != 0;
    }

    public ResultSet executeQuery() throws SQLException {
        checkExec();
        if (columnCount == 0)
            throw new SQLException("query does not return results");
        clearRS();
        db.reset(pointer); // TODO: needed?
        resultsWaiting = db.execute(this, batch);
        return getResultSet();
    }

    public int executeUpdate() throws SQLException {
        checkExec();
        if (columnCount != 0)
            throw new SQLException("query returns results");
        clearRS();
        db.reset(pointer);
        return db.executeUpdate(this, batch);
    }

    public int[] executeBatch() throws SQLException {
        return db.executeBatch(pointer, batchPos / paramCount, batch);
    }

    public int getUpdateCount() throws SQLException {
        checkOpen();
        if (pointer == 0 || resultsWaiting) return -1;
        return db.changes();
    }

    public void addBatch() throws SQLException {
        checkExec();
        batchPos += paramCount;
        if (batchPos + paramCount > batch.length) {
            Object[] nb = new Object[batch.length * 2];
            System.arraycopy(batch, 0, nb, 0, batch.length);
            batch = nb;
        }
    }

    public void clearBatch() throws SQLException { clearParameters(); }


    // ParameterMetaData FUNCTIONS //////////////////////////////////

    public ParameterMetaData getParameterMetaData() { return this; }

    public int getParameterCount() throws SQLException {
        checkExec(); return paramCount; }
    public String getParameterClassName(int param) throws SQLException {
        checkExec(); return "java.lang.String"; }
    public String getParameterTypeName(int pos) { return "VARCHAR"; }
    public int getParameterType(int pos) { return Types.VARCHAR; }
    public int getParameterMode(int pos) { return parameterModeIn; }
    public int getPrecision(int pos) { return 0; }
    public int getScale(int pos) { return 0; }
    public int isNullable(int pos) { return parameterNullable; }
    public boolean isSigned(int pos) { return true; }
    public Statement getStatement() { return this; }


    // PARAMETER FUNCTIONS //////////////////////////////////////////

    private void batch(int pos, Object value) throws SQLException {
        checkExec();
        if (batch == null) batch = new Object[paramCount];
        batch[batchPos + pos - 1] = value;
    }

    public void setBoolean(int pos, boolean value) throws SQLException {
        setInt(pos, value ? 1 : 0);
    }
    public void setByte(int pos, byte value) throws SQLException {
        setInt(pos, (int)value);
    }
    public void setBytes(int pos, byte[] value) throws SQLException {
        batch(pos, value);
    }
    public void setDouble(int pos, double value) throws SQLException {
        batch(pos, new Double(value));
    }
    public void setFloat(int pos, float value) throws SQLException {
        setDouble(pos, value);
    }
    public void setInt(int pos, int value) throws SQLException {
        batch(pos, new Integer(value));
    }
    public void setLong(int pos, long value) throws SQLException {
        batch(pos, new Long(value));
    }
    public void setNull(int pos, int u1) throws SQLException {
        setNull(pos, u1, null);
    }
    public void setNull(int pos, int u1, String u2) throws SQLException {
        batch(pos, null);
    }
    public void setObject(int pos , Object value) throws SQLException {
        if (value == null)
            batch(pos, null);
        else if (value instanceof java.util.Date)
            batch(pos, new Long(((java.util.Date)value).getTime()));
        else if (value instanceof Long) batch(pos, value);
        else if (value instanceof Integer) batch(pos, value);
        else if (value instanceof Float) batch(pos, value);
        else if (value instanceof Double) batch(pos, value);
        else
            batch(pos, value.toString());
    }
    public void setObject(int p, Object v, int t) throws SQLException {
        setObject(p, v); }
    public void setObject(int p, Object v, int t, int s) throws SQLException {
        setObject(p, v); }
    public void setShort(int pos, short value) throws SQLException {
        setInt(pos, (int)value); }
    public void setString(int pos, String value) throws SQLException {
        batch(pos, value);
    }
    public void setDate(int pos, Date x) throws SQLException {
        setLong(pos, x.getTime()); }
    public void setDate(int pos, Date x, Calendar cal) throws SQLException {
        setLong(pos, x.getTime()); }
    public void setTime(int pos, Time x) throws SQLException {
        setLong(pos, x.getTime()); }
    public void setTime(int pos, Time x, Calendar cal) throws SQLException {
        setLong(pos, x.getTime()); }
    public void setTimestamp(int pos, Timestamp x) throws SQLException {
        setLong(pos, x.getTime()); }
    public void setTimestamp(int pos, Timestamp x, Calendar cal)
        throws SQLException { setLong(pos, x.getTime()); }


    // UNUSED ///////////////////////////////////////////////////////

    public boolean execute(String sql)
        throws SQLException { throw unused(); }
    public int executeUpdate(String sql)
        throws SQLException { throw unused(); }
    public ResultSet executeQuery(String sql)
        throws SQLException { throw unused(); }
    public void addBatch(String sql)
        throws SQLException { throw unused(); }

    private SQLException unused() {
        return new SQLException("not supported by PreparedStatment");
    }
}
