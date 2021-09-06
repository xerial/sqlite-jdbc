/*--------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
package org.sqlite.javax;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import org.sqlite.SQLiteConnection;
import org.sqlite.core.DB;
import org.sqlite.jdbc4.JDBC4PooledConnection;
import org.sqlite.jdbc4.JDBC4PreparedStatement;
import org.sqlite.jdbc4.JDBC4Statement;

public class SQLitePooledConnection extends JDBC4PooledConnection {

    protected SQLiteConnection physicalConn;
    protected volatile Connection handleConn;

    protected List<ConnectionEventListener> listeners = new ArrayList<ConnectionEventListener>();

    /**
     * Constructor.
     *
     * @param physicalConn The physical Connection.
     */
    protected SQLitePooledConnection(SQLiteConnection physicalConn) {
        this.physicalConn = physicalConn;
    }

    public SQLiteConnection getPhysicalConn() {
        return physicalConn;
    }
    /** @see javax.sql.PooledConnection#close() */
    public void close() throws SQLException {
        if (handleConn != null) {
            listeners.clear();
            handleConn.close();
        }

        if (physicalConn != null) {
            try {
                physicalConn.close();
            } finally {
                physicalConn = null;
            }
        }
    }

    /** @see javax.sql.PooledConnection#getConnection() */
    public Connection getConnection() throws SQLException {
        if (handleConn != null) handleConn.close();

        handleConn =
                (Connection)
                        Proxy.newProxyInstance(
                                getClass().getClassLoader(),
                                new Class[] {Connection.class},
                                new InvocationHandler() {
                                    boolean isClosed;

                                    public Object invoke(Object proxy, Method method, Object[] args)
                                            throws Throwable {
                                        try {
                                            String name = method.getName();
                                            if ("close".equals(name)) {
                                                ConnectionEvent event =
                                                        new ConnectionEvent(
                                                                SQLitePooledConnection.this);

                                                for (int i = listeners.size() - 1; i >= 0; i--) {
                                                    listeners.get(i).connectionClosed(event);
                                                }

                                                if (!physicalConn.getAutoCommit()) {
                                                    physicalConn.rollback();
                                                }
                                                physicalConn.setAutoCommit(true);
                                                isClosed = true;

                                                return null; // don't close physical connection
                                            } else if ("isClosed".equals(name)) {
                                                if (!isClosed)
                                                    isClosed =
                                                            ((Boolean)
                                                                            method.invoke(
                                                                                    physicalConn,
                                                                                    args))
                                                                    .booleanValue();

                                                return isClosed;
                                            }

                                            if (isClosed) {
                                                throw new SQLException("Connection is closed");
                                            }

                                            return method.invoke(physicalConn, args);
                                        } catch (SQLException e) {
                                            if ("database connection closed"
                                                    .equals(e.getMessage())) {
                                                ConnectionEvent event =
                                                        new ConnectionEvent(
                                                                SQLitePooledConnection.this, e);

                                                for (int i = listeners.size() - 1; i >= 0; i--) {
                                                    listeners.get(i).connectionErrorOccurred(event);
                                                }
                                            }

                                            throw e;
                                        } catch (InvocationTargetException ex) {
                                            throw ex.getCause();
                                        }
                                    }
                                });

        return handleConn;
    }

    /**
     * @see javax.sql.PooledConnection#addConnectionEventListener(javax.sql.ConnectionEventListener)
     */
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    /**
     * @see
     *     javax.sql.PooledConnection#removeConnectionEventListener(javax.sql.ConnectionEventListener)
     */
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    public List<ConnectionEventListener> getListeners() {
        return listeners;
    }
}

class SQLitePooledConnectionHandle extends SQLiteConnection {
    private final SQLitePooledConnection parent;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    public SQLitePooledConnectionHandle(SQLitePooledConnection parent) {
        super(parent.getPhysicalConn().getDatabase());
        this.parent = parent;
    }

    @Override
    public Statement createStatement() throws SQLException {
        return new JDBC4Statement(this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return new JDBC4PreparedStatement(this, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return null;
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return null;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {}

    @Override
    public boolean getAutoCommit() throws SQLException {
        return false;
    }

    @Override
    public void commit() throws SQLException {}

    @Override
    public void rollback() throws SQLException {}

    @Override
    public void close() throws SQLException {
        ConnectionEvent event = new ConnectionEvent(parent);

        List<ConnectionEventListener> listeners = parent.getListeners();
        for (int i = listeners.size() - 1; i >= 0; i--) {
            listeners.get(i).connectionClosed(event);
        }

        if (!parent.getPhysicalConn().getAutoCommit()) {
            parent.getPhysicalConn().rollback();
        }
        parent.getPhysicalConn().setAutoCommit(true);
        isClosed.set(true);
    }

    @Override
    public boolean isClosed() {
        return isClosed.get();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return null;
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {}

    @Override
    public boolean isReadOnly() throws SQLException {
        return false;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {}

    @Override
    public String getCatalog() throws SQLException {
        return null;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {}

    @Override
    public int getTransactionIsolation() {
        return 0;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {}

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(
            String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return null;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return null;
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {}

    @Override
    public void setHoldability(int holdability) throws SQLException {}

    @Override
    public int getHoldability() throws SQLException {
        return 0;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return null;
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return null;
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {}

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {}

    @Override
    public Statement createStatement(
            int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(
            String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return null;
    }

    @Override
    public CallableStatement prepareCall(
            String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        return null;
    }

    @Override
    public Clob createClob() throws SQLException {
        return null;
    }

    @Override
    public Blob createBlob() throws SQLException {
        return null;
    }

    @Override
    public NClob createNClob() throws SQLException {
        return null;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return null;
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return false;
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {}

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {}

    @Override
    public String getClientInfo(String name) throws SQLException {
        return null;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return null;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return null;
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return null;
    }

    @Override
    public void setSchema(String schema) throws SQLException {}

    @Override
    public String getSchema() throws SQLException {
        return null;
    }

    @Override
    public void abort(Executor executor) throws SQLException {}

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {}

    @Override
    public int getNetworkTimeout() throws SQLException {
        return 0;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public int getBusyTimeout() {
        return 0;
    }

    @Override
    public void setBusyTimeout(int timeoutMillis) {}

    @Override
    public DB getDatabase() {
        return null;
    }
}
