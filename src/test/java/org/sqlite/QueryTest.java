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
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.BeforeClass;
import org.junit.Test;

public class QueryTest
{
    @BeforeClass
    public static void forName() throws Exception {
        Class.forName("org.sqlite.JDBC");
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    @Test
    public void createTable() throws Exception {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS sample " + "(id INTEGER PRIMARY KEY, descr VARCHAR(40))");
        stmt.close();

        stmt = conn.createStatement();
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM sample");
            rs.next();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        conn.close();

    }

    @Test
    public void setFloatTest() throws Exception {
        float f = 3.141597f;
        Connection conn = getConnection();

        conn.createStatement().execute("create table sample (data NOAFFINITY)");
        PreparedStatement prep = conn.prepareStatement("insert into sample values(?)");
        prep.setFloat(1, f);
        prep.executeUpdate();

        PreparedStatement stmt = conn.prepareStatement("select * from sample where data > ?");
        stmt.setObject(1, 3.0f);
        ResultSet rs = stmt.executeQuery();
        assertTrue(rs.next());
        float f2 = rs.getFloat(1);
        assertEquals(f, f2, 0.0000001);

    }

    @Test
    public void dateTimeTest() throws Exception {
        Connection conn = getConnection();

        conn.createStatement().execute("create table sample (start_time datetime)");

        Date now = new Date();
        String date = new SimpleDateFormat(SQLiteConfig.DEFAULT_DATE_STRING_FORMAT).format(now);

        conn.createStatement().execute("insert into sample values(" + now.getTime() + ")");
        conn.createStatement().execute("insert into sample values('" + date + "')");

        ResultSet rs = conn.createStatement().executeQuery("select * from sample");
        assertTrue(rs.next());
        assertEquals(now, rs.getDate(1));
        assertTrue(rs.next());
        assertEquals(now, rs.getDate(1));

        PreparedStatement stmt = conn.prepareStatement("insert into sample values(?)");
        stmt.setDate(1, new java.sql.Date(now.getTime()));
    }

    @Test
    public void viewTest() throws Exception {
        Connection conn = getConnection();
        Statement st1 = conn.createStatement();
        // drop table if it already exists

        String tableName = "sample";
        st1.execute("DROP TABLE IF EXISTS " + tableName);
        st1.close();
        Statement st2 = conn.createStatement();
        st2.execute("DROP VIEW IF EXISTS " + tableName);
        st2.close();

    }

    @Test
    public void timeoutTest() throws Exception {
        Connection conn = getConnection();
        Statement st1 = conn.createStatement();

        st1.setQueryTimeout(1);

        st1.close();
    }

    @Test
    public void concatTest() {

        Connection conn = null;
        try {
            // create a database connection
            conn = getConnection();
            Statement statement = conn.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            statement.executeUpdate("drop table if exists person");
            statement.executeUpdate("create table person (id integer, name string, shortname string)");
            statement.executeUpdate("insert into person values(1, 'leo','L')");
            statement.executeUpdate("insert into person values(2, 'yui','Y')");
            statement.executeUpdate("insert into person values(3, 'abc', null)");

            statement.executeUpdate("drop table if exists message");
            statement.executeUpdate("create table message (id integer, subject string)");
            statement.executeUpdate("insert into message values(1, 'Hello')");
            statement.executeUpdate("insert into message values(2, 'World')");

            statement.executeUpdate("drop table if exists mxp");
            statement.executeUpdate("create table mxp (pid integer, mid integer, type string)");
            statement.executeUpdate("insert into mxp values(1,1, 'F')");
            statement.executeUpdate("insert into mxp values(2,1,'T')");
            statement.executeUpdate("insert into mxp values(1,2, 'F')");
            statement.executeUpdate("insert into mxp values(2,2,'T')");
            statement.executeUpdate("insert into mxp values(3,2,'T')");

            ResultSet rs = statement
                    .executeQuery("select group_concat(ifnull(shortname, name)) from mxp, person where mxp.mid=2 and mxp.pid=person.id and mxp.type='T'");
            while (rs.next()) {
                // read the result set
                assertEquals("Y,abc", rs.getString(1));
            }
            rs = statement
                    .executeQuery("select group_concat(ifnull(shortname, name)) from mxp, person where mxp.mid=1 and mxp.pid=person.id and mxp.type='T'");
            while (rs.next()) {
                // read the result set
                assertEquals("Y", rs.getString(1));
            }

            PreparedStatement ps = conn
                    .prepareStatement("select group_concat(ifnull(shortname, name)) from mxp, person where mxp.mid=? and mxp.pid=person.id and mxp.type='T'");
            ps.clearParameters();
            ps.setInt(1, new Integer(2));
            rs = ps.executeQuery();
            while (rs.next()) {
                // read the result set
                assertEquals("Y,abc", rs.getString(1));
            }
            ps.clearParameters();
            ps.setInt(1, new Integer(2));
            rs = ps.executeQuery();
            while (rs.next()) {
                // read the result set
                assertEquals("Y,abc", rs.getString(1));
            }

        }
        catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
        }
        finally {
            try {
                if (conn != null)
                    conn.close();
            }
            catch (SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }

    }

}
