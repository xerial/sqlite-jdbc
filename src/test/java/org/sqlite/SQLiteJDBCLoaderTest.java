/*--------------------------------------------------------------------------
 *  Copyright 2007 Taro L. Saito
 *
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
//--------------------------------------
// sqlite-jdbc Project
//
// SQLiteJDBCLoaderTest.java
// Since: Oct 15, 2007
//
// $URL$ 
// $Author$
//--------------------------------------
package org.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SQLiteJDBCLoaderTest
{

    private Connection connection = null;

    @Before
    public void setUp() throws Exception
    {
        connection = null;
        Class.forName("org.sqlite.JDBC");
        // create a database connection
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    @After
    public void tearDown() throws Exception
    {
        if (connection != null)
            connection.close();
    }

    @Test
    public void query() throws ClassNotFoundException
    {
        try
        {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            statement.executeUpdate("create table person ( id integer, name string)");
            statement.executeUpdate("insert into person values(1, 'leo')");
            statement.executeUpdate("insert into person values(2, 'yui')");

            ResultSet rs = statement.executeQuery("select * from person order by id");
            while (rs.next())
            {
                // read the result set
                rs.getInt(1);
                rs.getString(2);
            }
        }
        catch (SQLException e)
        {
            // if e.getMessage() is "out of memory", it probably means no
            // database file is found
            fail(e.getMessage());
        }
    }

    @Test
    public void function() throws SQLException
    {
        Function.create(connection, "total", new Function() {
            @Override
            protected void xFunc() throws SQLException
            {
                int sum = 0;
                for (int i = 0; i < args(); i++)
                    sum += value_int(i);
                result(sum);
            }
        });

        ResultSet rs = connection.createStatement().executeQuery("select total(1, 2, 3, 4, 5)");
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), 1 + 2 + 3 + 4 + 5);
    }

    @Test
    public void version()
    {
    // System.out.println(SQLiteJDBCLoader.getVersion());
    }

}
