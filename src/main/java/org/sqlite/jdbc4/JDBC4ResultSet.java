package org.sqlite.jdbc4;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Map;

import org.sqlite.core.CoreStatement;
import org.sqlite.jdbc3.JDBC3ResultSet;

public class JDBC4ResultSet extends JDBC3ResultSet implements ResultSet, ResultSetMetaData {
    public JDBC4ResultSet(CoreStatement stmt) {
        super(stmt);
    }

    // JDBC 4
    public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    public RowId getRowId(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public RowId getRowId(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public int getHoldability() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean isClosed() throws SQLException {
        return !isOpen();
    }

    public void updateNString(int columnIndex, String nString)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateNString(String columnLabel, String nString)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateNClob(String columnLabel, NClob nClob)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public NClob getNClob(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public NClob getNClob(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public void updateSQLXML(int columnIndex, SQLXML xmlObject)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateSQLXML(String columnLabel, SQLXML xmlObject)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public String getNString(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getNString(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public void updateNCharacterStream(int columnIndex, Reader x, long length)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateNCharacterStream(String columnLabel, Reader reader,
            long length) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateAsciiStream(int columnIndex, InputStream x, long length)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateBinaryStream(int columnIndex, InputStream x, long length)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateCharacterStream(int columnIndex, Reader x, long length)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateAsciiStream(String columnLabel, InputStream x, long length)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateBinaryStream(String columnLabel, InputStream x,
            long length) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateCharacterStream(String columnLabel, Reader reader,
            long length) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateBlob(int columnIndex, InputStream inputStream, long length)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateBlob(String columnLabel, InputStream inputStream,
            long length) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateClob(int columnIndex, Reader reader, long length)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateClob(String columnLabel, Reader reader, long length)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateNClob(int columnIndex, Reader reader, long length)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateNClob(String columnLabel, Reader reader, long length)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateNCharacterStream(int columnIndex, Reader x)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateNCharacterStream(String columnLabel, Reader reader)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateAsciiStream(int columnIndex, InputStream x)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateBinaryStream(int columnIndex, InputStream x)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateCharacterStream(int columnIndex, Reader x)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateAsciiStream(String columnLabel, InputStream x)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateBinaryStream(String columnLabel, InputStream x)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateCharacterStream(String columnLabel, Reader reader)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateBlob(int columnIndex, InputStream inputStream)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateBlob(String columnLabel, InputStream inputStream)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateClob(String columnLabel, Reader reader)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public void updateNClob(String columnLabel, Reader reader)
            throws SQLException {
        // TODO Auto-generated method stub
        
    }
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        // TODO
        return null;
    }
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        // TODO
        return null;
    }

    protected SQLException unused() {
        return new SQLException("not implemented by SQLite JDBC driver");
    }


    // ResultSet ////////////////////////////////////////////////////

    public Array getArray(int i)
        throws SQLException { throw unused(); }
    public Array getArray(String col)
        throws SQLException { throw unused(); }
    public InputStream getAsciiStream(int col)
        throws SQLException { throw unused(); }
    public InputStream getAsciiStream(String col)
        throws SQLException { throw unused(); }
//    public BigDecimal getBigDecimal(int col)
//        throws SQLException { throw unused(); }
    public BigDecimal getBigDecimal(int col, int s)
        throws SQLException { throw unused(); }
//    public BigDecimal getBigDecimal(String col)
//        throws SQLException { throw unused(); }
    public BigDecimal getBigDecimal(String col, int s)
        throws SQLException { throw unused(); }
    public Blob getBlob(int col)
        throws SQLException { throw unused(); }
    public Blob getBlob(String col)
        throws SQLException { throw unused(); }
    public Clob getClob(int col)
        throws SQLException { throw unused(); }
    public Clob getClob(String col)
        throws SQLException { throw unused(); }
    @SuppressWarnings("rawtypes")
    public Object getObject(int col, Map map)
        throws SQLException { throw unused(); }
    @SuppressWarnings("rawtypes")
    public Object getObject(String col, Map map)
        throws SQLException { throw unused(); }
    public Ref getRef(int i)
        throws SQLException { throw unused(); }
    public Ref getRef(String col)
        throws SQLException { throw unused(); }

    public InputStream getUnicodeStream(int col)
        throws SQLException { throw unused(); }
    public InputStream getUnicodeStream(String col)
        throws SQLException { throw unused(); }
    public URL getURL(int col)
        throws SQLException { throw unused(); }
    public URL getURL(String col)
        throws SQLException { throw unused(); }

    public void insertRow() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public void moveToCurrentRow() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public void moveToInsertRow() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public boolean last() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public boolean previous() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public boolean relative(int rows) throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public boolean absolute(int row) throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public void afterLast() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public void beforeFirst() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public boolean first() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }

    public void cancelRowUpdates()
        throws SQLException { throw unused(); }
    public void deleteRow()
        throws SQLException { throw unused(); }

    public void updateArray(int col, Array x)
        throws SQLException { throw unused(); }
    public void updateArray(String col, Array x)
        throws SQLException { throw unused(); }
    public void updateAsciiStream(int col, InputStream x, int l)
        throws SQLException { throw unused(); }
    public void updateAsciiStream(String col, InputStream x, int l)
        throws SQLException { throw unused(); }
    public void updateBigDecimal(int col, BigDecimal x)
        throws SQLException { throw unused(); }
    public void updateBigDecimal(String col, BigDecimal x)
        throws SQLException { throw unused(); }
    public void updateBinaryStream(int c, InputStream x, int l)
        throws SQLException { throw unused(); }
    public void updateBinaryStream(String c, InputStream x, int l)
        throws SQLException { throw unused(); }
    public void updateBlob(int col, Blob x)
        throws SQLException { throw unused(); }
    public void updateBlob(String col, Blob x)
        throws SQLException { throw unused(); }
    public void updateBoolean(int col, boolean x)
        throws SQLException { throw unused(); }
    public void updateBoolean(String col, boolean x)
        throws SQLException { throw unused(); }
    public void updateByte(int col, byte x)
        throws SQLException { throw unused(); }
    public void updateByte(String col, byte x)
        throws SQLException { throw unused(); }
    public void updateBytes(int col, byte[] x)
        throws SQLException { throw unused(); }
    public void updateBytes(String col, byte[] x)
        throws SQLException { throw unused(); }
    public void updateCharacterStream(int c, Reader x, int l)
        throws SQLException { throw unused(); }
    public void updateCharacterStream(String c, Reader r, int l)
        throws SQLException { throw unused(); }
    public void updateClob(int col, Clob x)
        throws SQLException { throw unused(); }
    public void updateClob(String col, Clob x)
        throws SQLException { throw unused(); }
    public void updateDate(int col, Date x)
        throws SQLException { throw unused(); }
    public void updateDate(String col, Date x)
        throws SQLException { throw unused(); }
    public void updateDouble(int col, double x)
        throws SQLException { throw unused(); }
    public void updateDouble(String col, double x)
        throws SQLException { throw unused(); }
    public void updateFloat(int col, float x)
        throws SQLException { throw unused(); }
    public void updateFloat(String col, float x)
        throws SQLException { throw unused(); }
    public void updateInt(int col, int x)
        throws SQLException { throw unused(); }
    public void updateInt(String col, int x)
        throws SQLException { throw unused(); }
    public void updateLong(int col, long x)
        throws SQLException { throw unused(); }
    public void updateLong(String col, long x)
        throws SQLException { throw unused(); }
    public void updateNull(int col)
        throws SQLException { throw unused(); }
    public void updateNull(String col)
        throws SQLException { throw unused(); }
    public void updateObject(int c, Object x)
        throws SQLException { throw unused(); }
    public void updateObject(int c, Object x, int s)
        throws SQLException { throw unused(); }
    public void updateObject(String col, Object x)
        throws SQLException { throw unused(); }
    public void updateObject(String c, Object x, int s)
        throws SQLException { throw unused(); }
    public void updateRef(int col, Ref x)
        throws SQLException { throw unused(); }
    public void updateRef(String c, Ref x)
        throws SQLException { throw unused(); }
    public void updateRow()
        throws SQLException { throw unused(); }
    public void updateShort(int c, short x)
        throws SQLException { throw unused(); }
    public void updateShort(String c, short x)
        throws SQLException { throw unused(); }
    public void updateString(int c, String x)
        throws SQLException { throw unused(); }
    public void updateString(String c, String x)
        throws SQLException { throw unused(); }
    public void updateTime(int c, Time x)
        throws SQLException { throw unused(); }
    public void updateTime(String c, Time x)
        throws SQLException { throw unused(); }
    public void updateTimestamp(int c, Timestamp x)
        throws SQLException { throw unused(); }
    public void updateTimestamp(String c, Timestamp x)
        throws SQLException { throw unused(); }

    public void refreshRow()
        throws SQLException { throw unused(); }
}
