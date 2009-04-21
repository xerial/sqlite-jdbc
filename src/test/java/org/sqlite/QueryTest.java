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

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

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

    @Test
    public void setFloatTest() throws Exception
    {
        String driver = "org.sqlite.JDBC";
        String url = "jdbc:sqlite::memory:";

        float f = 3.141597f;
        Connection conn = DriverManager.getConnection(url);
        conn.createStatement().execute("create table sample (data NOAFFINITY)");
        conn.createStatement().execute(String.format("insert into sample values(%f)", f));

        PreparedStatement stmt = conn.prepareStatement("select * from sample where data > ?");
        stmt.setObject(1, 3.0f);
        ResultSet rs = stmt.executeQuery();
        assertTrue(rs.next());
        float f2 = rs.getFloat(1);
        assertEquals(f, f2, 0.0000001);

    }

    @Test
    public void dateTimeTest() throws Exception
    {
        String url = "jdbc:sqlite::memory:";

        float f = 3.141597f;
        Connection conn = DriverManager.getConnection(url);
        conn.createStatement().execute("create table sample (start_time datetime)");

        Date now = new Date();
        //String date = "2000-01-01 16:45:00";
        conn.createStatement().execute(String.format("insert into sample values(%s)", now.getTime()));

        ResultSet rs = conn.createStatement().executeQuery("select * from sample");
        assertTrue(rs.next());
        assertEquals(now, rs.getDate(1));

    }

    @Test
    public void viewTest() throws Exception
    {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        Statement st1 = conn.createStatement();
        // drop table if it already exists

        String tableName = "sample";
        st1.execute("DROP TABLE IF EXISTS " + tableName);
        st1.close();
        Statement st2 = conn.createStatement();
        st2.execute("DROP VIEW IF EXISTS " + tableName);
        st2.close();

    }

}
