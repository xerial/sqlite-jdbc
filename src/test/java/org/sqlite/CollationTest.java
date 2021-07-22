package org.sqlite;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Collator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests User Defined Collations.
 */
public class CollationTest {
    private Connection conn;
    private Statement stat;

    @Before
    public void connect() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
    }

    @After
    public void close() throws SQLException {
        stat.close();
        conn.close();
    }

    @Test
    public void reverseCollation() throws SQLException {
        Collation.create(conn, "REVERSE", new Collation() {
            @Override
            protected int xCompare(String str1, String str2) {
                System.out.println("string 1:" + str1);
                System.out.println("string 2:" + str2);
                return str1.compareTo(str2) * -1;
            }
        });
        stat.executeUpdate("create table t (c1);");
        stat.executeUpdate("insert into t values ('aaa');");
        stat.executeUpdate("insert into t values ('aba');");
        stat.executeUpdate("insert into t values ('aca');");
        ResultSet rs = stat.executeQuery("select c1 from t order by c1 collate REVERSE;");
        assertTrue(rs.next());
        assertEquals(rs.getString(1), "aca");
        assertTrue(rs.next());
        assertEquals(rs.getString(1), "aba");
        assertTrue(rs.next());
        assertEquals(rs.getString(1), "aaa");
    }

    @Test
    public void unicodeCollation() throws SQLException {
        Collation.create(conn, "UNICODE", new Collation() {
            @Override
            protected int xCompare(String str1, String str2) {
                System.out.println("string 1:" + str1);
                System.out.println("string 2:" + str2);

                Collator collator = Collator.getInstance();
                collator.setDecomposition(Collator.TERTIARY);
                collator.setStrength(Collator.CANONICAL_DECOMPOSITION);

                return collator.compare(str1, str2);
            }
        });
        stat.executeUpdate("create table t (c1);");
        stat.executeUpdate("insert into t values ('aec');");
        stat.executeUpdate("insert into t values ('aea');");
        stat.executeUpdate("insert into t values ('aéb');");
        ResultSet rs = stat.executeQuery("select c1 from t order by c1 collate UNICODE;");
        assertTrue(rs.next());
        assertEquals(rs.getString(1), "aea");
        assertTrue(rs.next());
        assertEquals(rs.getString(1), "aéb");
        assertTrue(rs.next());
        assertEquals(rs.getString(1), "aec");
    }
}
