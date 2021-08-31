package org.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExtensionTest {
    Connection conn;
    Statement stat;

    @BeforeEach
    public void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (stat != null) {
            stat.close();
        }
        if (conn != null) {
            conn.close();
        }
    }

    @Test
    public void extFTS3() throws Exception {
        stat.execute("create virtual table recipe using fts3(name, ingredients)");
        stat.execute(
                "insert into recipe (name, ingredients) values('broccoli stew', 'broccoli peppers cheese tomatoes')");
        stat.execute(
                "insert into recipe (name, ingredients) values('pumpkin stew', 'pumpkin onions garlic celery')");

        ResultSet rs =
                stat.executeQuery(
                        "select rowid, name, ingredients from recipe where ingredients match 'onions'");
        assertTrue(rs.next());
        assertEquals("pumpkin stew", rs.getString(2));
    }

    @Test
    public void extFTS5() throws Exception {
        stat.execute("create virtual table recipe using fts5(name, ingredients)");
        stat.execute(
                "insert into recipe (name, ingredients) values('broccoli stew', 'broccoli peppers cheese tomatoes')");
        stat.execute(
                "insert into recipe (name, ingredients) values('pumpkin stew', 'pumpkin onions garlic celery')");

        ResultSet rs =
                stat.executeQuery(
                        "select rowid, name, ingredients from recipe where recipe match 'onions'");
        assertTrue(rs.next());
        assertEquals("pumpkin stew", rs.getString(2));
    }

    @Test
    public void extFunctions() throws Exception {
        {
            ResultSet rs = stat.executeQuery("pragma compile_options");
            boolean hasJdbcExtensions = false;
            while (rs.next()) {
                String compileOption = rs.getString(1);
                if (compileOption.equals("JDBC_EXTENSIONS")) {
                    hasJdbcExtensions = true;
                    break;
                }
            }
            rs.close();
            // SQLite has to be compiled with JDBC Extensions for this test to
            // continue.
            assumeTrue(hasJdbcExtensions);
        }
        {
            ResultSet rs = stat.executeQuery("select cos(radians(45))");
            assertTrue(rs.next());
            assertEquals(0.707106781186548, rs.getDouble(1), 0.000000000000001);
            rs.close();
        }

        {
            ResultSet rs = stat.executeQuery("select reverse(\"ACGT\")");
            assertTrue(rs.next());
            assertEquals("TGCA", rs.getString(1));
            rs.close();
        }
    }
}
