/* Copyright 2006 David Crawshaw, see LICENSE file for licensing [BSD]. */
package org.sqlite;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;

/** See comment in RS.java to explain the strange inheritance hierarchy. */
final class PrepStmt extends RS implements PreparedStatement, ParameterMetaData, Codes
{
    private int columnCount;
    private int paramCount;
    private int batchPos;
    private Object[] batch;

    PrepStmt(Conn conn, String sql) throws SQLException
    {
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
    public void close() throws SQLException
    {
        batch = null;
        if (pointer == 0 || db == null)
            clearRS();
        else
            clearParameters();
    }

    public void clearParameters() throws SQLException
    {
        checkOpen();
        clearRS();
        db.reset(pointer);
        batchPos = 0;
        if (batch != null)
            for (int i = 0; i < batch.length; i++)
                batch[i] = null;
    }

    protected void finalize() throws SQLException
    {
        db.finalize(this);
        // TODO
    }

    public boolean execute() throws SQLException
    {
        checkExec();
        clearRS();
        db.reset(pointer); // TODO: needed?
        resultsWaiting = db.execute(this, batch);
        return columnCount != 0;
    }

    public ResultSet executeQuery() throws SQLException
    {
        checkExec();
        if (columnCount == 0)
            throw new SQLException("query does not return results");
        clearRS();
        db.reset(pointer); // TODO: needed?
        resultsWaiting = db.execute(this, batch);
        return getResultSet();
    }

    public int executeUpdate() throws SQLException
    {
        checkExec();
        if (columnCount != 0)
            throw new SQLException("query returns results");
        clearRS();
        db.reset(pointer);
        return db.executeUpdate(this, batch);
    }

    public int[] executeBatch() throws SQLException
    {
        return db.executeBatch(pointer, batchPos / paramCount, batch);
    }

    public int getUpdateCount() throws SQLException
    {
        checkOpen();
        if (pointer == 0 || resultsWaiting)
            return -1;
        return db.changes();
    }

    public void addBatch() throws SQLException
    {
        checkExec();
        batchPos += paramCount;
        if (batchPos + paramCount > batch.length)
        {
            Object[] nb = new Object[batch.length * 2];
            System.arraycopy(batch, 0, nb, 0, batch.length);
            batch = nb;
        }
    }

    public void clearBatch() throws SQLException
    {
        clearParameters();
    }

    // ParameterMetaData FUNCTIONS //////////////////////////////////

    public ParameterMetaData getParameterMetaData()
    {
        return this;
    }

    public int getParameterCount() throws SQLException
    {
        checkExec();
        return paramCount;
    }

    public String getParameterClassName(int param) throws SQLException
    {
        checkExec();
        return "java.lang.String";
    }

    public String getParameterTypeName(int pos)
    {
        return "VARCHAR";
    }

    public int getParameterType(int pos)
    {
        return Types.VARCHAR;
    }

    public int getParameterMode(int pos)
    {
        return parameterModeIn;
    }

    public int getPrecision(int pos)
    {
        return 0;
    }

    public int getScale(int pos)
    {
        return 0;
    }

    public int isNullable(int pos)
    {
        return parameterNullable;
    }

    public boolean isSigned(int pos)
    {
        return true;
    }

    public Statement getStatement()
    {
        return this;
    }

    // PARAMETER FUNCTIONS //////////////////////////////////////////

    private void batch(int pos, Object value) throws SQLException
    {
        checkExec();
        if (batch == null)
            batch = new Object[paramCount];
        batch[batchPos + pos - 1] = value;
    }

    public void setBoolean(int pos, boolean value) throws SQLException
    {
        setInt(pos, value ? 1 : 0);
    }

    public void setByte(int pos, byte value) throws SQLException
    {
        setInt(pos, (int) value);
    }

    public void setBytes(int pos, byte[] value) throws SQLException
    {
        batch(pos, value);
    }

    public void setDouble(int pos, double value) throws SQLException
    {
        batch(pos, new Double(value));
    }

    public void setFloat(int pos, float value) throws SQLException
    {
        setDouble(pos, value);
    }

    public void setInt(int pos, int value) throws SQLException
    {
        batch(pos, new Integer(value));
    }

    public void setLong(int pos, long value) throws SQLException
    {
        batch(pos, new Long(value));
    }

    public void setNull(int pos, int u1) throws SQLException
    {
        setNull(pos, u1, null);
    }

    public void setNull(int pos, int u1, String u2) throws SQLException
    {
        batch(pos, null);
    }

    public void setObject(int pos, Object value) throws SQLException
    {
        // TODO: catch wrapped primitives
        batch(pos, value == null ? null : value.toString());
    }

    public void setObject(int p, Object v, int t) throws SQLException
    {
        setObject(p, v);
    }

    public void setObject(int p, Object v, int t, int s) throws SQLException
    {
        setObject(p, v);
    }

    public void setShort(int pos, short value) throws SQLException
    {
        setInt(pos, (int) value);
    }

    public void setString(int pos, String value) throws SQLException
    {
        batch(pos, value);
    }

    public void setDate(int pos, Date x) throws SQLException
    {
        setLong(pos, x.getTime());
    }

    public void setDate(int pos, Date x, Calendar cal) throws SQLException
    {
        setLong(pos, x.getTime());
    }

    public void setTime(int pos, Time x) throws SQLException
    {
        setLong(pos, x.getTime());
    }

    public void setTime(int pos, Time x, Calendar cal) throws SQLException
    {
        setLong(pos, x.getTime());
    }

    public void setTimestamp(int pos, Timestamp x) throws SQLException
    {
        setLong(pos, x.getTime());
    }

    public void setTimestamp(int pos, Timestamp x, Calendar cal) throws SQLException
    {
        setLong(pos, x.getTime());
    }

    // UNUSED ///////////////////////////////////////////////////////

    public boolean execute(String sql) throws SQLException
    {
        throw unused();
    }

    public int executeUpdate(String sql) throws SQLException
    {
        throw unused();
    }

    public ResultSet executeQuery(String sql) throws SQLException
    {
        throw unused();
    }

    public void addBatch(String sql) throws SQLException
    {
        throw unused();
    }

    private SQLException unused()
    {
        return new SQLException("not supported by PreparedStatment");
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public boolean isClosed() throws SQLException
    {
        throw new SQLException("not yet implemented");
    }

    @Override
    public boolean isPoolable() throws SQLException
    {
        throw new SQLException("not yet implemented");
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public boolean isWrapperFor(Class< ? > iface) throws SQLException
    {
        throw new SQLException("not yet implemented");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        throw new SQLException("not yet implemented");
    }

    @Override
    public int getHoldability() throws SQLException
    {
        throw new SQLException("not yet implemented");
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException
    {
        throw new SQLException("not yet implemented");
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException
    {
        throw new SQLException("not yet implemented");
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException
    {
        throw new SQLException("not yet implemented");
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException
    {
        throw new SQLException("not yet implemented");

    }

    @Override
    public String getNString(int columnIndex) throws SQLException
    {
        throw new SQLException("not yet implemented");
    }

    @Override
    public String getNString(String columnLabel) throws SQLException
    {
        throw new SQLException("not yet implemented");
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException
    {
        throw new SQLException("not yet implemented");

    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException
    {
        throw new SQLException("not yet implemented");

    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException
    {
        throw new SQLException("not yet implemented");
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException
    {
        throw new SQLException("not yet implemented");
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateNClob(int columnIndex, NClob clob) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateNClob(String columnLabel, NClob clob) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateNString(int columnIndex, String string) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateNString(String columnLabel, String string) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new SQLException("not yet implemented");

    }
}
