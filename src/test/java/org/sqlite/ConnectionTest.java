package org.sqlite;

import static org.junit.Assert.*;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sqlite.SQLiteConfig.SynchronousMode;
import org.xerial.util.FileResource;

/**
 * These tests check whether access to files is woring correctly and some
 * Connection.close() cases.
 */
public class ConnectionTest
{
    @BeforeClass
    public static void forName() throws Exception {
        Class.forName("org.sqlite.JDBC");
    }

    @Test
    public void executeUpdateOnClosedDB() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        Statement stat = conn.createStatement();
        conn.close();

        try {
            stat.executeUpdate("create table A(id, name)");
        }
        catch (SQLException e) {
            return; // successfully detect the operation on the closed DB
        }
        fail("should not reach here");
    }

    @Test
    public void readOnly() throws SQLException {

        // set read only mode
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);

        Connection conn = DriverManager.getConnection("jdbc:sqlite:", config.toProperties());
        Statement stat = conn.createStatement();
        try {
            assertTrue(conn.isReadOnly());
            // these updates must be forbidden in read-only mode
            stat.executeUpdate("create table A(id, name)");
            stat.executeUpdate("insert into A values(1, 'leo')");

        }
        catch (SQLException e) {
            return; // success
        }
        finally {
            stat.close();
            conn.close();
        }
        fail("read only flag is not properly set");
    }

    @Test
    public void foreignKeys() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        Connection conn = DriverManager.getConnection("jdbc:sqlite:", config.toProperties());
        Statement stat = conn.createStatement();

        try {
            stat.executeUpdate("create table track(id integer primary key, name, aid, foreign key (aid) references artist(id))");
            stat.executeUpdate("create table artist(id integer primary key, name)");

            stat.executeUpdate("insert into artist values(10, 'leo')");
            stat.executeUpdate("insert into track values(1, 'first track', 10)"); // OK

            try {
                stat.executeUpdate("insert into track values(2, 'second track', 3)"); // invalid reference
            }
            catch (SQLException e) {
                return; // successfully detect violation of foreign key constraints
            }
            fail("foreign key constraint must be enforced");
        }
        finally {
            stat.close();
            conn.close();
        }

    }

    @Test
    public void canWrite() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        Connection conn = DriverManager.getConnection("jdbc:sqlite:", config.toProperties());
        Statement stat = conn.createStatement();

        try {
            assertFalse(conn.isReadOnly());
        }
        finally {
            stat.close();
            conn.close();
        }

    }

    @Test
    public void synchronous() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.setSynchronous(SynchronousMode.OFF);
        Connection conn = DriverManager.getConnection("jdbc:sqlite:", config.toProperties());
        Statement stat = conn.createStatement();

        try {
            ResultSet rs = stat.executeQuery("pragma synchronous");
            if (rs.next()) {
                ResultSetMetaData rm = rs.getMetaData();
                int i = rm.getColumnCount();
                int synchronous = rs.getInt(1);
                assertEquals(0, synchronous);
            }

        }
        finally {
            stat.close();
            conn.close();
        }

    }

    @Test
    public void openMemory() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        conn.close();
    }

    @Test
    public void isClosed() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        conn.close();
        assertTrue(conn.isClosed());
    }

    @Test
    public void openFile() throws Exception {

        File testDB = FileResource.copyToTemp(ConnectionTest.class, "sample.db", new File("target"));
        testDB.deleteOnExit();
        assertTrue(testDB.exists());
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", testDB));
        conn.close();
    }

    @Test(expected = SQLException.class)
    public void closeTest() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        PreparedStatement prep = conn.prepareStatement("select null;");
        ResultSet rs = prep.executeQuery();
        conn.close();
        prep.clearParameters();
    }

    @Test(expected = SQLException.class)
    public void openInvalidLocation() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:/");
        conn.close();
    }

    @Test
    public void openResource() throws Exception {
        File testDB = FileResource.copyToTemp(ConnectionTest.class, "sample.db", new File("target"));
        testDB.deleteOnExit();
        assertTrue(testDB.exists());
        Connection conn = DriverManager
                .getConnection(String.format("jdbc:sqlite::resource:%s", testDB.toURI().toURL()));
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("select * from coordinate");
        assertTrue(rs.next());
        rs.close();
        stat.close();
        conn.close();

    }

    @Test
    public void openJARResource() throws Exception {
        File testJAR = FileResource.copyToTemp(ConnectionTest.class, "testdb.jar", new File("target"));
        testJAR.deleteOnExit();
        assertTrue(testJAR.exists());

        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite::resource:jar:%s!/sample.db", testJAR
                .toURI().toURL()));
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("select * from coordinate");
        assertTrue(rs.next());
        rs.close();
        stat.close();
        conn.close();
    }

}
