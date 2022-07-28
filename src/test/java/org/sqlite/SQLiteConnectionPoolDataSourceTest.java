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
package org.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

public class SQLiteConnectionPoolDataSourceTest {

    @Test
    public void connectionTest() throws SQLException {
        ConnectionPoolDataSource ds = new SQLiteConnectionPoolDataSource();

        PooledConnection pooledConn = ds.getPooledConnection();

        Connection handle = pooledConn.getConnection();
        assertFalse(handle.isClosed());
        assertTrue(handle.createStatement().execute("select 1"));

        Connection handle2 = pooledConn.getConnection();
        assertTrue(handle.isClosed());
        try {
            handle.createStatement().execute("select 1");
            fail();
        } catch (SQLException e) {
            assertEquals("Connection is closed", e.getMessage());
        }

        assertTrue(handle2.createStatement().execute("select 1"));
        handle2.close();

        handle = pooledConn.getConnection();
        assertTrue(handle.createStatement().execute("select 1"));

        pooledConn.close();
        assertTrue(handle.isClosed());
    }

    @Disabled
    @Test
    public void proxyConnectionCloseTest() throws SQLException {
        ConnectionPoolDataSource ds = new SQLiteConnectionPoolDataSource();
        PooledConnection pooledConn = ds.getPooledConnection();
        System.out.println("pooledConn: " + pooledConn.getClass());

        Connection handle = pooledConn.getConnection();
        System.out.println("pooledConn.getConnection: " + handle.getClass());

        Statement st = handle.createStatement();
        System.out.println("statement: " + st.getClass());
        Connection stConn = handle.createStatement().getConnection();
        System.out.println("statement connection:" + stConn.getClass());
        stConn.close(); // This closes the physical connection, not the proxy

        Connection handle2 = pooledConn.getConnection();
    }
}
