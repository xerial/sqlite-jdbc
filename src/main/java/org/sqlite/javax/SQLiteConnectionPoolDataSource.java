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
import java.sql.SQLException;
import javax.sql.PooledConnection;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

public class SQLiteConnectionPoolDataSource extends SQLiteDataSource implements javax.sql.ConnectionPoolDataSource {

    /**
     * Default constructor.
     */
    public SQLiteConnectionPoolDataSource () {
        super();
    }

    /**
     * Creates a data source based on the provided configuration.
     * @param config The configuration for the data source.
     */
    public SQLiteConnectionPoolDataSource(SQLiteConfig config) {
        super(config);
    }

    /**
     * @see javax.sql.ConnectionPoolDataSource#getPooledConnection()
     */
    public PooledConnection getPooledConnection() throws SQLException {
        return getPooledConnection(null, null);
    }

    /**
     * @see javax.sql.ConnectionPoolDataSource#getPooledConnection(java.lang.String, java.lang.String)
     */
    public PooledConnection getPooledConnection(String user, String password) throws SQLException {
        return new SQLitePooledConnection(getConnection(user, password));
    }
}