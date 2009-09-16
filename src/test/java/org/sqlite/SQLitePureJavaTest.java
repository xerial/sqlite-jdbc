//--------------------------------------
// sqlite-jdbc Project
//
// SQLiteNestedTest.java
// Since: Jul 17, 2008
//
// $URL$ 
// $Author$
//--------------------------------------
package org.sqlite;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SQLitePureJavaTest
{

    private Connection connection = null;

    @Before
    public void setUp() throws Exception {
        System.setProperty("sqlite.purejava", "true");
        connection = null;
        Class.forName("org.sqlite.JDBC");
        // create a database connection
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null)
            connection.close();

        System.setProperty("sqlite.purejava", "false");
    }

    @Test
    public void query() throws ClassNotFoundException {
        //_logger.debug(String.format("running in %s mode", SQLiteJDBCLoader.isNativeMode() ? "native" : "nested"));

        assertTrue(SQLiteJDBCLoader.isNativeMode() == false);

        try {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            statement.executeUpdate("create table person ( id integer, name string)");
            statement.executeUpdate("insert into person values(1, 'leo')");
            statement.executeUpdate("insert into person values(2, 'yui')");

            ResultSet rs = statement.executeQuery("select * from person order by id");
            while (rs.next()) {
                // read the result set
                int id = rs.getInt(1);
                String name = rs.getString(2);
            }
        }
        catch (SQLException e) {
            // if e.getMessage() is "out of memory", it probably means no
            // database file is found
            fail(e.getMessage());
        }
    }

    @Test
    public void function() throws SQLException {
        assertTrue(SQLiteJDBCLoader.isNativeMode() == false);

        Function.create(connection, "total", new Function() {
            @Override
            protected void xFunc() throws SQLException {
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

}
