package org.sqlite;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExtensionTest
{

    @BeforeClass
    public static void forName() throws Exception {
        Class.forName("org.sqlite.JDBC");
    }

    Connection conn;
    Statement  stat;

    @Before
    public void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
    }

    @After
    public void tearDown() throws Exception {
        if (stat != null)
            stat.close();
        if (conn != null)
            conn.close();

    }

    @Test
    public void extFTS3() throws Exception {

        stat.execute("create virtual table recipe using fts3(name, ingredients)");
        stat
                .execute("insert into recipe (name, ingredients) values('broccoli stew', 'broccoli peppers cheese tomatoes')");
        stat.execute("insert into recipe (name, ingredients) values('pumpkin stew', 'pumpkin onions garlic celery')");

        ResultSet rs = stat
                .executeQuery("select rowid, name, ingredients from recipe where ingredients match 'onions'");
        assertTrue(rs.next());
        assertEquals("pumpkin stew", rs.getString(2));

    }

    @Test
    public void extFunctions() throws Exception {

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
