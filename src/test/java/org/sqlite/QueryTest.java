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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.junit.Test;
import org.sqlite.date.FastDateFormat;

public class QueryTest
{
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    @Test
    public void nullQuery() throws Exception {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
	try {
		stmt.execute(null);
	} catch (NullPointerException e) {
	}
        stmt.close();
        conn.close();
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
        String date = FastDateFormat.getInstance(SQLiteConfig.DEFAULT_DATE_STRING_FORMAT).format(now);

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
    public void dateTimeWithTimeZoneTest() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(SQLiteConfig.Pragma.DATE_CLASS.pragmaName, "text");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:", properties);

        Statement statement = null;
        try {
            statement = conn.createStatement();
            statement.execute("create table sample (date_time datetime)");
        }
        finally {
            if (statement != null) statement.close();
        }

        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        TimeZone customTimeZone = TimeZone.getTimeZone("+3");
        Calendar utcCalendar = Calendar.getInstance(utcTimeZone);
        Calendar customCalendar = Calendar.getInstance(customTimeZone);

        java.sql.Date now = new java.sql.Date(new Date().getTime());
        FastDateFormat customFormat = FastDateFormat.getInstance(SQLiteConfig.DEFAULT_DATE_STRING_FORMAT, customTimeZone);
        FastDateFormat utcFormat = FastDateFormat.getInstance(SQLiteConfig.DEFAULT_DATE_STRING_FORMAT, utcTimeZone);
        java.sql.Date nowLikeCustomZoneIsUtc = new java.sql.Date(utcFormat.parse(customFormat.format(now)).getTime());

        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("insert into sample (date_time) values(?)");
            preparedStatement.setDate(1, now, customCalendar);
            preparedStatement.executeUpdate();
            preparedStatement.setDate(1, nowLikeCustomZoneIsUtc, utcCalendar);
            preparedStatement.executeUpdate();
        }
        finally {
            if (preparedStatement != null) preparedStatement.close();
        }

        ResultSet resultSet = null;
        try {
            resultSet = conn.createStatement().executeQuery("select * from sample");
            assertTrue(resultSet.next());
            assertEquals(now, resultSet.getDate(1, customCalendar));
            assertEquals(nowLikeCustomZoneIsUtc, resultSet.getDate(1, utcCalendar));

            assertTrue(resultSet.next());
            assertEquals(now, resultSet.getDate(1, customCalendar));
            assertEquals(nowLikeCustomZoneIsUtc, resultSet.getDate(1, utcCalendar));
        }
        finally {
            if (resultSet != null) resultSet.close();
        }
    }

    @Test
    public void notEmptyBlob() throws Exception {
        Connection conn = getConnection();

        conn.createStatement().execute("create table sample (b blob not null)");

        conn.createStatement().execute("insert into sample values(zeroblob(5))");

        ResultSet rs = conn.createStatement().executeQuery("select * from sample");
        assertTrue(rs.next());
        assertEquals(5, rs.getBytes(1).length);
        assertFalse(rs.wasNull());
    }

    @Test
    public void emptyBlob() throws Exception {
        Connection conn = getConnection();

        conn.createStatement().execute("create table sample (b blob null)");

        conn.createStatement().execute("insert into sample values(zeroblob(0))");

        ResultSet rs = conn.createStatement().executeQuery("select * from sample");
        assertTrue(rs.next());
        assertEquals(0, rs.getBytes(1).length);
        assertFalse(rs.wasNull());
    }

    @Test
    public void nullBlob() throws Exception {
        Connection conn = getConnection();

        conn.createStatement().execute("create table sample (b blob null)");

        conn.createStatement().execute("insert into sample values(null)");

        ResultSet rs = conn.createStatement().executeQuery("select * from sample");
        assertTrue(rs.next());
        assertNull(rs.getBytes(1));
        assertTrue(rs.wasNull());
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
