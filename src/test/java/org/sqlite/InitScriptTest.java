package org.sqlite;

import org.junit.Test;

import java.sql.*;
import java.util.Properties;

import static org.junit.Assert.*;

public class InitScriptTest {
    @Test
    public void classPath() throws SQLException {
        Properties prop = new Properties();
        prop.setProperty("INIT", "classpath: init.sql");

        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:", prop);
        Statement stat = conn.createStatement();

        ResultSet rs = stat.executeQuery("SELECT * FROM Foo");
        assertTrue(rs.next());
        assertEquals(9, rs.getInt(1));
        assertFalse(rs.next());
        rs.close();

        stat.close();
        conn.close();
    }

    @Test
    public void file() throws SQLException {
        Properties prop = new Properties();
        prop.setProperty("INIT", "file: target/test-classes/init.sql");

        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:", prop);
        Statement stat = conn.createStatement();

        ResultSet rs = stat.executeQuery("SELECT * FROM Foo");
        assertTrue(rs.next());
        assertEquals(9, rs.getInt(1));
        assertFalse(rs.next());
        rs.close();

        stat.close();
        conn.close();
    }

    @Test
    public void script() throws SQLException {
        Properties prop = new Properties();
        prop.setProperty("INIT", "CREATE TABLE Foo (bar INTEGER NOT NULL PRIMARY KEY); INSERT INTO Foo (bar) VALUES (9);");

        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:", prop);
        Statement stat = conn.createStatement();

        ResultSet rs = stat.executeQuery("SELECT * FROM Foo");
        assertTrue(rs.next());
        assertEquals(9, rs.getInt(1));
        assertFalse(rs.next());
        rs.close();

        stat.close();
        conn.close();
    }
}
