package org.sqlite.jdbc4;

import java.sql.SQLException;
import java.sql.Statement;

import org.sqlite.SQLiteConnection;
import org.sqlite.jdbc3.JDBC3Statement;

public class JDBC4Statement extends JDBC3Statement implements Statement {
    public JDBC4Statement(SQLiteConnection conn) {
        super(conn);
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

    public boolean isClosed() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    public void setPoolable(boolean poolable) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    public boolean isPoolable() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }
    public void closeOnCompletion() throws SQLException {
        // TODO
    }
    public boolean isCloseOnCompletion() throws SQLException {
        // TODO
        return false;
    }
}
