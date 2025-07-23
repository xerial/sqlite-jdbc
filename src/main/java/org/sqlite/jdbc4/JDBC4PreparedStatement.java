package org.sqlite.jdbc4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.util.Arrays;
import org.sqlite.SQLiteConnection;
import org.sqlite.jdbc3.JDBC3PreparedStatement;

public class JDBC4PreparedStatement extends JDBC3PreparedStatement
        implements PreparedStatement, ParameterMetaData {

    @Override
    public String toString() {
        return sql + " \n parameters=" + Arrays.toString(batch);
    }

    public JDBC4PreparedStatement(SQLiteConnection conn, String sql) throws SQLException {
        super(conn, sql);
    }

    // JDBC 4
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }

    public void setNString(int parameterIndex, String value) throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length)
            throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }

    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }

    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        requireLengthIsPositiveInt(length);
        setCharacterStream(parameterIndex, reader, (int) length);
    }

    private void requireLengthIsPositiveInt(long length) throws SQLFeatureNotSupportedException {
        if (length > Integer.MAX_VALUE || length < 0) {
            throw new SQLFeatureNotSupportedException(
                    "Data must have a length between 0 and Integer.MAX_VALUE");
        }
    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length)
            throws SQLException {
        requireLengthIsPositiveInt(length);
        setBinaryStream(parameterIndex, inputStream, (int) length);
    }

    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }

    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        requireLengthIsPositiveInt(length);
        setAsciiStream(parameterIndex, x, (int) length);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, long length)
            throws SQLException {
        requireLengthIsPositiveInt(length);
        setBinaryStream(parameterIndex, x, (int) length);
    }

    public void setCharacterStream(int parameterIndex, Reader reader, long length)
            throws SQLException {
        requireLengthIsPositiveInt(length);
        setCharacterStream(parameterIndex, reader, (int) length);
    }

    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        byte[] bytes = readBytes(x);
        setAsciiStream(parameterIndex, new ByteArrayInputStream(bytes), bytes.length);
    }

    /**
     * Reads given number of bytes from an input stream.
     *
     * @param istream The input stream.
     * @param length The number of bytes to read.
     * @return byte array.
     * @throws SQLException
     */
    private byte[] readBytes(InputStream istream) throws SQLException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytes = new byte[8192];

        try {
            int bytesRead;
            while ((bytesRead = istream.read(bytes)) > 0) {
                baos.write(bytes, 0, bytesRead);
            }
            return baos.toByteArray();
        } catch (IOException cause) {
            SQLException exception = new SQLException("Error reading stream");

            exception.initCause(cause);
            throw exception;
        }
    }

    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        setBytes(parameterIndex, readBytes(x));
    }

    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        setCharacterStream(parameterIndex, reader, Integer.MAX_VALUE);
    }

    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }

    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        setCharacterStream(parameterIndex, reader, Integer.MAX_VALUE);
    }

    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        setBytes(parameterIndex, readBytes(inputStream));
    }

    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }
}
