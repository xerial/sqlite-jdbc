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
    public <T> T unwrap(Class<T> iface) throws ClassCastException {
        return iface.cast(this);
    }

    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }

    private boolean closed = false;

    @Override
    public void close() throws SQLException {
        super.close();
        closed = true; // isClosed() should only return true when close() happened
    }

    public boolean isClosed() {
        return closed;
    }

    boolean closeOnCompletion;

    public void closeOnCompletion() throws SQLException {
        if (closed) throw new SQLException("statement is closed");
        closeOnCompletion = true;
    }

    public boolean isCloseOnCompletion() throws SQLException {
        if (closed) throw new SQLException("statement is closed");
        return closeOnCompletion;
    }

    public void setPoolable(boolean poolable) throws SQLException {
        // TODO Auto-generated method stub

    }

    public boolean isPoolable() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }
}
