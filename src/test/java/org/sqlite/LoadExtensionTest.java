package org.sqlite;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Andy-2639
 */
public class LoadExtensionTest {

    public static final String LIBTEST = "target/test-classes/libtest.so";
    public static final String LIBTEST2 = "target/test-classes/libtest2.so";

    @BeforeClass
    public static void beforeClass() {
        assumeTrue((new File(LIBTEST)).exists());
        assumeTrue((new File(LIBTEST2)).exists());
    }

    public SQLException execute(Connection conn, String sql) throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            try {
                stmt.execute(sql);
                return null;
            } catch (SQLException se) {
                return se;
            }
        } finally {
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
        }
    }

    @Test
    public void testFunctionsNotDefined() throws SQLException {
        Connection conn = null;
        try {
            SQLiteConfig config = new SQLiteConfig();
            conn = config.createConnection("jdbc:sqlite:");
            assertNotNull(this.execute(conn, "SELECT test()"));
            assertNotNull(this.execute(conn, "SELECT testa()"));
            assertNotNull(this.execute(conn, "SELECT test2()"));
            assertNotNull(this.execute(conn, "SELECT test3()"));
        } finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
    }

    @Test
    public void testLoadSqlNotAllowed() throws SQLException {
        Connection conn = null;
        try {
            SQLiteConfig config = new SQLiteConfig();
            conn = config.createConnection("jdbc:sqlite:");
            assertNotNull(this.execute(conn, "SELECT load_extension('" + LIBTEST + "');"));
        } finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
    }

    @Test
    public void testLoadSqlAllowed() throws SQLException {
        Connection conn = null;
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.enableLoadExtension(true);
            conn = config.createConnection("jdbc:sqlite:");
            assertNull(this.execute(conn, "SELECT load_extension('" + LIBTEST + "');"));
            assertNull(this.execute(conn, "SELECT test();"));
            assertNotNull(this.execute(conn, "SELECT testa();"));
        } finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
    }

    @Test
    public void testLoadSqlAllowed2() throws SQLException {
        Connection conn = null;
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.enableLoadExtension(true);
            conn = config.createConnection("jdbc:sqlite:");
            assertNull(this.execute(conn, "SELECT load_extension('" + LIBTEST + "', 'sqlite3_testa_init');"));
            assertNull(this.execute(conn, "SELECT testa();"));
            assertNotNull(this.execute(conn, "SELECT test();"));
        } finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
    }

    @Test
    public void testLoadCApi() throws SQLException {
        Connection conn = null;
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.loadExtension(LIBTEST);
            conn = config.createConnection("jdbc:sqlite:");
            assertNull(this.execute(conn, "SELECT test();"));
            assertNotNull(this.execute(conn, "SELECT testa();"));
        } finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
    }

    @Test
    public void testLoadCApi2() throws SQLException {
        Connection conn = null;
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.loadExtension(LIBTEST, "sqlite3_testa_init");
            conn = config.createConnection("jdbc:sqlite:");
            assertNull(this.execute(conn, "SELECT testa();"));
            assertNotNull(this.execute(conn, "SELECT test();"));
        } finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
    }

    @Test
    public void testLoadCApiForbiddenSql() throws SQLException {
        Connection conn = null;
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.loadExtension(LIBTEST);
            conn = config.createConnection("jdbc:sqlite:");
            assertNull(this.execute(conn, "SELECT test();"));
            assertNotNull(this.execute(conn, "SELECT load_extension('" + LIBTEST2 + "');"));
        } finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
    }

    @Test
    public void testLoadCApiSql() throws SQLException {
        Connection conn = null;
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.enableLoadExtension(true);
            config.loadExtension(LIBTEST);
            conn = config.createConnection("jdbc:sqlite:");
            assertNull(this.execute(conn, "SELECT test();"));
            assertNull(this.execute(conn, "SELECT load_extension('" + LIBTEST2 + "');"));
            assertNull(this.execute(conn, "SELECT test2();"));
            assertNotNull(this.execute(conn, "SELECT test3();"));
        } finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
    }

    @Test
    public void testLoadCApiMultiple() throws SQLException {
        Connection conn = null;
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.loadExtension(LIBTEST);
            config.loadExtension(LIBTEST, "sqlite3_testa_init");
            config.loadExtension(LIBTEST2);
            conn = config.createConnection("jdbc:sqlite:");
            assertNull(this.execute(conn, "SELECT test();"));
            assertNull(this.execute(conn, "SELECT testa();"));
            assertNull(this.execute(conn, "SELECT test2();"));
            assertNotNull(this.execute(conn, "SELECT test3();"));
        } finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
    }

}
