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

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import org.sqlite.jdbc4.JDBC4PooledConnection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLitePooledConnection extends JDBC4PooledConnection implements PooledConnection {

    protected Connection physicalConn;
    protected volatile Connection handleConn;

    protected List<ConnectionEventListener> listeners = new ArrayList<ConnectionEventListener>();

    /**
     * Constructor.
     * @param physicalConn The physical Connection.
     */
    protected SQLitePooledConnection(Connection physicalConn) {
        this.physicalConn = physicalConn;
    }

    /**
     * @see javax.sql.PooledConnection#close()
     */
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

    /**
     * @see javax.sql.PooledConnection#getConnection()
     */
    public Connection getConnection() throws SQLException {
        if (handleConn != null)
            handleConn.close();

        handleConn = (Connection)Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Connection.class},
            new InvocationHandler() {
                boolean isClosed;

                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    try {
                        String name = method.getName();
                        if ("close".equals(name)) {
                            ConnectionEvent event = new ConnectionEvent(SQLitePooledConnection.this);

                            for (int i = listeners.size() - 1; i >= 0; i--) {
                                listeners.get(i).connectionClosed(event);
                            }

                            if (!physicalConn.getAutoCommit()) {
                                physicalConn.rollback();
                            }
                            physicalConn.setAutoCommit(true);
                            isClosed = true;

                            return null; // don't close physical connection
                        }
                        else if ("isClosed".equals(name)) {
                            if (!isClosed)
                                isClosed = ((Boolean)method.invoke(physicalConn, args)).booleanValue();

                            return isClosed;
                        }

                        if (isClosed) {
                            throw new SQLException ("Connection is closed");
                        }

                        return method.invoke(physicalConn, args);
                    }
                    catch (SQLException e){
                        if ("database connection closed".equals(e.getMessage())) {
                            ConnectionEvent event = new ConnectionEvent(SQLitePooledConnection.this, e);

                            for (int i = listeners.size() - 1; i >= 0; i--) {
                                listeners.get(i).connectionErrorOccurred(event);
                            }
                        }

                        throw e;
                    }
                    catch (InvocationTargetException ex) {
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
     * @see javax.sql.PooledConnection#removeConnectionEventListener(javax.sql.ConnectionEventListener)
     */
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }
}