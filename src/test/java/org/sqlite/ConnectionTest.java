package org.sqlite;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

            fail("read only flag is not properly set");
        }
        catch (SQLException e) {
            // success
        }
        finally {
            stat.close();
            conn.close();
        }

        config.setReadOnly(true); // should be a no-op

        try{
            conn.setReadOnly(false);
            fail("should not change read only flag after opening connection");
        }
        catch (SQLException e) {
           assert(e.getMessage().contains("Cannot change read-only flag after establishing a connection.")); 
        }
        finally {
            conn.close();
        }
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
        File testDB = copyToTemp("sample.db");
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
        File testJAR = copyToTemp("testdb.jar");
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

    @Test
    public void openFile() throws Exception {

        File testDB = copyToTemp("sample.db");

        assertTrue(testDB.exists());
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", testDB));
        conn.close();
    }

    public static File copyToTemp(String fileName) throws IOException {
        InputStream in = ConnectionTest.class.getResourceAsStream(fileName);
        File dir = new File("target");
        if (!dir.exists())
            dir.mkdirs();

        File tmp = File.createTempFile(fileName, "", new File("target"));
        tmp.deleteOnExit();
        FileOutputStream out = new FileOutputStream(tmp);

        byte[] buf = new byte[8192];
        for (int readBytes = 0; (readBytes = in.read(buf)) != -1;) {
            out.write(buf, 0, readBytes);
        }
        out.flush();
        out.close();
        in.close();

        return tmp;
    }

}
