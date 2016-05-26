package org.sqlite.jdbc4;

import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.util.Properties;

import org.sqlite.SQLiteConnection;
import org.sqlite.jdbc3.JDBC3Connection;

public abstract class JDBC4Connection extends JDBC3Connection implements Connection {

    public JDBC4Connection(String url, String fileName, Properties prop) throws SQLException
    {
        super(url, fileName, prop);
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        checkOpen();

        if (meta == null) {
            meta = new JDBC4DatabaseMetaData((SQLiteConnection)this);
        }

        return (DatabaseMetaData)meta;
    }

    public Statement createStatement(int rst, int rsc, int rsh) throws SQLException {
        checkOpen();
        checkCursor(rst, rsc, rsh);

        return new JDBC4Statement((SQLiteConnection)this);
    }

    public PreparedStatement prepareStatement(String sql, int rst, int rsc, int rsh) throws SQLException {
        checkOpen();
        checkCursor(rst, rsc, rsh);

        return new JDBC4PreparedStatement((SQLiteConnection)this, sql);
    }

    //JDBC 4
    /**
     * @see java.sql.Connection#isClosed()
     */
    public boolean isClosed() throws SQLException {
        return db == null;
    }

    public <T> T unwrap(Class<T> iface) throws ClassCastException {
        // caller should invoke isWrapperFor prior to unwrap
        return iface.cast(this);
    }

    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }

    public Clob createClob() throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }

    public Blob createBlob() throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }

    public NClob createNClob() throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }

    public SQLXML createSQLXML() throws SQLException {
        // TODO Support this
        throw new SQLFeatureNotSupportedException();
    }

    public boolean isValid(int timeout) throws SQLException {
        if (db == null) {
            return false;
        }
        Statement statement = createStatement();
        try {
            return statement.execute("select 1");
        } finally {
            statement.close();
        }
    }

    public void setClientInfo(String name, String value)
            throws SQLClientInfoException {
        // TODO Auto-generated method stub

    }

    public void setClientInfo(Properties properties)
            throws SQLClientInfoException {
        // TODO Auto-generated method stub

    }

    public String getClientInfo(String name) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public Properties getClientInfo() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public Array createArrayOf(String typeName, Object[] elements)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
}
