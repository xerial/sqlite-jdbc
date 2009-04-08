//--------------------------------------
// sqlite-jdbc Project
//
// QueryTest.java
// Since: Apr 8, 2009
//
// $URL$ 
// $Author$
//--------------------------------------
package org.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.BeforeClass;
import org.junit.Test;

public class QueryTest
{
    @BeforeClass
    public static void forName() throws Exception
    {
        Class.forName("org.sqlite.JDBC");
    }

    @Test
    public void createTable() throws Exception
    {
        String driver = "org.sqlite.JDBC";
        String url = "jdbc:sqlite::memory:";
        //String url = "jdbc:sqlite:file.db";
        Class.forName(driver);
        Connection conn = DriverManager.getConnection(url);
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS sample " + "(id INTEGER PRIMARY KEY, descr VARCHAR(40))");
        stmt.close();

        stmt = conn.createStatement();
        try
        {
            ResultSet rs = stmt.executeQuery("SELECT * FROM sample");
            rs.next();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        conn.close();

    }

}
