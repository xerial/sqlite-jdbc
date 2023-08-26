package org.sqlite.jdbc3;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.JDBCType;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import org.sqlite.SQLiteConnection;
import org.sqlite.core.CorePreparedStatement;
import org.sqlite.core.DB;

public abstract class JDBC3PreparedStatement extends CorePreparedStatement {

    protected JDBC3PreparedStatement(SQLiteConnection conn, String sql) throws SQLException {
        super(conn, sql);
    }

    /** @see java.sql.PreparedStatement#clearParameters() */
    public void clearParameters() throws SQLException {
        checkOpen();
        pointer.safeRunConsume(DB::clear_bindings);
        if (batch != null) for (int i = batchPos; i < batchPos + paramCount; i++) batch[i] = null;
    }

    /** @see java.sql.PreparedStatement#execute() */
    public boolean execute() throws SQLException {
        checkOpen();
        rs.close();
        pointer.safeRunConsume(DB::reset);
        exhaustedResults = false;

        if (this.conn instanceof JDBC3Connection) {
            ((JDBC3Connection) this.conn).tryEnforceTransactionMode();
        }

        return this.withConnectionTimeout(
                () -> {
                    boolean success = false;
                    try {
                        resultsWaiting =
                                conn.getDatabase().execute(JDBC3PreparedStatement.this, batch);
                        success = true;
                        updateCount = getDatabase().changes();
                        return 0 != columnCount;
                    } finally {
                        if (!success && !pointer.isClosed()) pointer.safeRunConsume(DB::reset);
                    }
                });
    }

    /** @see java.sql.PreparedStatement#executeQuery() */
    public ResultSet executeQuery() throws SQLException {
        checkOpen();

        if (columnCount == 0) {
            throw new SQLException("Query does not return results");
        }

        rs.close();
        pointer.safeRunConsume(DB::reset);
        exhaustedResults = false;

        if (this.conn instanceof JDBC3Connection) {
            ((JDBC3Connection) this.conn).tryEnforceTransactionMode();
        }

        return this.withConnectionTimeout(
                () -> {
                    boolean success = false;
                    try {
                        resultsWaiting =
                                conn.getDatabase().execute(JDBC3PreparedStatement.this, batch);
                        success = true;
                    } finally {
                        if (!success && !pointer.isClosed()) {
                            pointer.safeRunInt(DB::reset);
                        }
                    }
                    return getResultSet();
                });
    }

    /** @see java.sql.PreparedStatement#executeUpdate() */
    public int executeUpdate() throws SQLException {
        return (int) executeLargeUpdate();
    }

    /** @see java.sql.PreparedStatement#executeLargeUpdate() */
    public long executeLargeUpdate() throws SQLException {
        checkOpen();

        if (columnCount != 0) {
            throw new SQLException("Query returns results");
        }

        rs.close();
        pointer.safeRunConsume(DB::reset);
        exhaustedResults = false;

        if (this.conn instanceof JDBC3Connection) {
            ((JDBC3Connection) this.conn).tryEnforceTransactionMode();
        }

        return this.withConnectionTimeout(
                () -> conn.getDatabase().executeUpdate(JDBC3PreparedStatement.this, batch));
    }

    /** @see java.sql.PreparedStatement#addBatch() */
    public void addBatch() throws SQLException {
        checkOpen();
        batchPos += paramCount;
        batchQueryCount++;
        if (batch == null) {
            batch = new Object[paramCount];
        }
        if (batchPos + paramCount > batch.length) {
            Object[] nb = new Object[batch.length * 2];
            System.arraycopy(batch, 0, nb, 0, batch.length);
            batch = nb;
        }
        System.arraycopy(batch, batchPos - paramCount, batch, batchPos, paramCount);
    }

    // ParameterMetaData FUNCTIONS //////////////////////////////////

    /** @see java.sql.PreparedStatement#getParameterMetaData() */
    public ParameterMetaData getParameterMetaData() {
        return (ParameterMetaData) this;
    }

    /** @see java.sql.ParameterMetaData#getParameterCount() */
    public int getParameterCount() throws SQLException {
        checkOpen();
        return paramCount;
    }

    /** @see java.sql.ParameterMetaData#getParameterClassName(int) */
    public String getParameterClassName(int param) throws SQLException {
        checkOpen();
        return "java.lang.String";
    }

    /** @see java.sql.ParameterMetaData#getParameterTypeName(int) */
    public String getParameterTypeName(int pos) throws SQLException {
        checkIndex(pos);
        return JDBCType.valueOf(getParameterType(pos)).getName();
    }

    /** @see java.sql.ParameterMetaData#getParameterType(int) */
    public int getParameterType(int pos) throws SQLException {
        checkIndex(pos);
        Object paramValue = batch[pos - 1];

        if (paramValue == null) {
            return Types.NULL;
        } else if (paramValue instanceof Integer
                || paramValue instanceof Short
                || paramValue instanceof Boolean) {
            return Types.INTEGER;
        } else if (paramValue instanceof Long) {
            return Types.BIGINT;
        } else if (paramValue instanceof Double || paramValue instanceof Float) {
            return Types.REAL;
        } else {
            return Types.VARCHAR;
        }
    }

    /** @see java.sql.ParameterMetaData#getParameterMode(int) */
    public int getParameterMode(int pos) {
        return ParameterMetaData.parameterModeIn;
    }

    /** @see java.sql.ParameterMetaData#getPrecision(int) */
    public int getPrecision(int pos) {
        return 0;
    }

    /** @see java.sql.ParameterMetaData#getScale(int) */
    public int getScale(int pos) {
        return 0;
    }

    /** @see java.sql.ParameterMetaData#isNullable(int) */
    public int isNullable(int pos) {
        return ParameterMetaData.parameterNullable;
    }

    /** @see java.sql.ParameterMetaData#isSigned(int) */
    public boolean isSigned(int pos) {
        return true;
    }

    /** @return */
    public Statement getStatement() {
        return this;
    }

    /** @see java.sql.PreparedStatement#setBigDecimal(int, java.math.BigDecimal) */
    public void setBigDecimal(int pos, BigDecimal value) throws SQLException {
        batch(pos, value == null ? null : value.toString());
    }

    /**
     * Reads given number of bytes from an input stream.
     *
     * @param istream The input stream.
     * @param length The number of bytes to read.
     * @return byte array.
     * @throws SQLException
     */
    private byte[] readBytes(InputStream istream, int length) throws SQLException {
        if (length < 0) {
            throw new SQLException("Error reading stream. Length should be non-negative");
        }

        byte[] bytes = new byte[length];

        try {
            int bytesRead;
            int totalBytesRead = 0;

            while (totalBytesRead < length) {
                bytesRead = istream.read(bytes, totalBytesRead, length - totalBytesRead);
                if (bytesRead == -1) {
                    throw new IOException("End of stream has been reached");
                }
                totalBytesRead += bytesRead;
            }

            return bytes;
        } catch (IOException cause) {
            SQLException exception = new SQLException("Error reading stream");

            exception.initCause(cause);
            throw exception;
        }
    }

    /** @see java.sql.PreparedStatement#setBinaryStream(int, java.io.InputStream, int) */
    public void setBinaryStream(int pos, InputStream istream, int length) throws SQLException {
        if (istream == null && length == 0) {
            setBytes(pos, null);
        }

        setBytes(pos, readBytes(istream, length));
    }

    /** @see java.sql.PreparedStatement#setAsciiStream(int, java.io.InputStream, int) */
    public void setAsciiStream(int pos, InputStream istream, int length) throws SQLException {
        setUnicodeStream(pos, istream, length);
    }

    /** @see java.sql.PreparedStatement#setUnicodeStream(int, java.io.InputStream, int) */
    public void setUnicodeStream(int pos, InputStream istream, int length) throws SQLException {
        if (istream == null && length == 0) {
            setString(pos, null);
        }

        try {
            setString(pos, new String(readBytes(istream, length), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            SQLException exception = new SQLException("UTF-8 is not supported");

            exception.initCause(e);
            throw exception;
        }
    }

    /** @see java.sql.PreparedStatement#setBoolean(int, boolean) */
    public void setBoolean(int pos, boolean value) throws SQLException {
        setInt(pos, value ? 1 : 0);
    }

    /** @see java.sql.PreparedStatement#setByte(int, byte) */
    public void setByte(int pos, byte value) throws SQLException {
        setInt(pos, value);
    }

    /** @see java.sql.PreparedStatement#setBytes(int, byte[]) */
    public void setBytes(int pos, byte[] value) throws SQLException {
        batch(pos, value);
    }

    /** @see java.sql.PreparedStatement#setDouble(int, double) */
    public void setDouble(int pos, double value) throws SQLException {
        batch(pos, new Double(value));
    }

    /** @see java.sql.PreparedStatement#setFloat(int, float) */
    public void setFloat(int pos, float value) throws SQLException {
        batch(pos, new Float(value));
    }

    /** @see java.sql.PreparedStatement#setInt(int, int) */
    public void setInt(int pos, int value) throws SQLException {
        batch(pos, new Integer(value));
    }

    /** @see java.sql.PreparedStatement#setLong(int, long) */
    public void setLong(int pos, long value) throws SQLException {
        batch(pos, new Long(value));
    }

    /** @see java.sql.PreparedStatement#setNull(int, int) */
    public void setNull(int pos, int u1) throws SQLException {
        setNull(pos, u1, null);
    }

    /** @see java.sql.PreparedStatement#setNull(int, int, java.lang.String) */
    public void setNull(int pos, int u1, String u2) throws SQLException {
        batch(pos, null);
    }

    /** @see java.sql.PreparedStatement#setObject(int, java.lang.Object) */
    public void setObject(int pos, Object value) throws SQLException {
        if (value == null) {
            batch(pos, null);
        } else if (value instanceof java.util.Date) {
            setDateByMilliseconds(pos, ((java.util.Date) value).getTime(), Calendar.getInstance());
        } else if (value instanceof Long) {
            batch(pos, value);
        } else if (value instanceof Integer) {
            batch(pos, value);
        } else if (value instanceof Short) {
            batch(pos, new Integer(((Short) value).intValue()));
        } else if (value instanceof Float) {
            batch(pos, value);
        } else if (value instanceof Double) {
            batch(pos, value);
        } else if (value instanceof Boolean) {
            setBoolean(pos, ((Boolean) value).booleanValue());
        } else if (value instanceof byte[]) {
            batch(pos, value);
        } else if (value instanceof BigDecimal) {
            setBigDecimal(pos, (BigDecimal) value);
        } else {
            batch(pos, value.toString());
        }
    }

    /** @see java.sql.PreparedStatement#setObject(int, java.lang.Object, int) */
    public void setObject(int p, Object v, int t) throws SQLException {
        setObject(p, v);
    }

    /** @see java.sql.PreparedStatement#setObject(int, java.lang.Object, int, int) */
    public void setObject(int p, Object v, int t, int s) throws SQLException {
        setObject(p, v);
    }

    /** @see java.sql.PreparedStatement#setShort(int, short) */
    public void setShort(int pos, short value) throws SQLException {
        setInt(pos, value);
    }

    /** @see java.sql.PreparedStatement#setString(int, java.lang.String) */
    public void setString(int pos, String value) throws SQLException {
        batch(pos, value);
    }

    /** @see java.sql.PreparedStatement#setCharacterStream(int, java.io.Reader, int) */
    public void setCharacterStream(int pos, Reader reader, int length) throws SQLException {
        try {
            // copy chars from reader to StringBuffer
            StringBuffer sb = new StringBuffer();
            char[] cbuf = new char[8192];
            int cnt;

            while ((cnt = reader.read(cbuf)) > 0) {
                sb.append(cbuf, 0, cnt);
            }

            // set as string
            setString(pos, sb.toString());
        } catch (IOException e) {
            throw new SQLException(
                    "Cannot read from character stream, exception message: " + e.getMessage());
        }
    }

    /** @see java.sql.PreparedStatement#setDate(int, java.sql.Date) */
    public void setDate(int pos, Date x) throws SQLException {
        setDate(pos, x, Calendar.getInstance());
    }

    /** @see java.sql.PreparedStatement#setDate(int, java.sql.Date, java.util.Calendar) */
    public void setDate(int pos, Date x, Calendar cal) throws SQLException {
        if (x == null) {
            setObject(pos, null);
        } else {
            setDateByMilliseconds(pos, x.getTime(), cal);
        }
    }

    /** @see java.sql.PreparedStatement#setTime(int, java.sql.Time) */
    public void setTime(int pos, Time x) throws SQLException {
        setTime(pos, x, Calendar.getInstance());
    }

    /** @see java.sql.PreparedStatement#setTime(int, java.sql.Time, java.util.Calendar) */
    public void setTime(int pos, Time x, Calendar cal) throws SQLException {
        if (x == null) {
            setObject(pos, null);
        } else {
            setDateByMilliseconds(pos, x.getTime(), cal);
        }
    }

    /** @see java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp) */
    public void setTimestamp(int pos, Timestamp x) throws SQLException {
        setTimestamp(pos, x, Calendar.getInstance());
    }

    /** @see java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp, java.util.Calendar) */
    public void setTimestamp(int pos, Timestamp x, Calendar cal) throws SQLException {
        if (x == null) {
            setObject(pos, null);
        } else {
            setDateByMilliseconds(pos, x.getTime(), cal);
        }
    }

    /** @see java.sql.PreparedStatement#getMetaData() */
    public ResultSetMetaData getMetaData() throws SQLException {
        checkOpen();
        return (ResultSetMetaData) rs;
    }

    protected SQLException unsupported() {
        return new SQLFeatureNotSupportedException("not implemented by SQLite JDBC driver");
    }

    protected SQLException invalid() {
        return new SQLException("method cannot be called on a PreparedStatement");
    }

    // PreparedStatement ////////////////////////////////////////////

    public void setArray(int i, Array x) throws SQLException {
        throw unsupported();
    }

    public void setBlob(int i, Blob x) throws SQLException {
        throw unsupported();
    }

    public void setClob(int i, Clob x) throws SQLException {
        throw unsupported();
    }

    public void setRef(int i, Ref x) throws SQLException {
        throw unsupported();
    }

    public void setURL(int pos, URL x) throws SQLException {
        throw unsupported();
    }

    /** @see org.sqlite.core.CoreStatement#exec(java.lang.String) */
    @Override
    public boolean execute(String sql) throws SQLException {
        throw invalid();
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        throw invalid();
    }

    public boolean execute(String sql, int[] colinds) throws SQLException {
        throw invalid();
    }

    public boolean execute(String sql, String[] colnames) throws SQLException {
        throw invalid();
    }

    /** @see org.sqlite.core.CoreStatement#exec(java.lang.String) */
    @Override
    public int executeUpdate(String sql) throws SQLException {
        throw invalid();
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw invalid();
    }

    public int executeUpdate(String sql, int[] colinds) throws SQLException {
        throw invalid();
    }

    public int executeUpdate(String sql, String[] cols) throws SQLException {
        throw invalid();
    }

    public long executeLargeUpdate(String sql) throws SQLException {
        throw invalid();
    }

    public long executeLargeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw invalid();
    }

    public long executeLargeUpdate(String sql, int[] colinds) throws SQLException {
        throw invalid();
    }

    public long executeLargeUpdate(String sql, String[] cols) throws SQLException {
        throw invalid();
    }

    /** @see org.sqlite.core.CoreStatement#exec(String) */
    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        throw invalid();
    }

    /** */
    @Override
    public void addBatch(String sql) throws SQLException {
        throw invalid();
    }
}
