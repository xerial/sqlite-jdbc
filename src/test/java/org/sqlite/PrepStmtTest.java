package org.sqlite;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** These tests are designed to stress PreparedStatements on memory dbs. */
public class PrepStmtTest
{
    static byte[]      b1    = new byte[] { 1, 2, 7, 4, 2, 6, 2, 8, 5, 2, 3, 1, 5, 3, 6, 3, 3, 6, 2, 5 };
    static byte[]      b2    = "To be or not to be.".getBytes();
    static byte[]      b3    = "Question!#$%".getBytes();
    static String      utf01 = "\uD840\uDC40";
    static String      utf02 = "\uD840\uDC47 ";
    static String      utf03 = " \uD840\uDC43";
    static String      utf04 = " \uD840\uDC42 ";
    static String      utf05 = "\uD840\uDC40\uD840\uDC44";
    static String      utf06 = "Hello World, \uD840\uDC40 \uD880\uDC99";
    static String      utf07 = "\uD840\uDC41 testing \uD880\uDC99";
    static String      utf08 = "\uD840\uDC40\uD840\uDC44 testing";

    private Connection conn;
    private Statement  stat;

    @BeforeClass
    public static void forName() throws Exception {
        Class.forName("org.sqlite.JDBC");
    }

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
    public void update() throws SQLException {
        assertEquals(conn.prepareStatement("create table s1 (c1);").executeUpdate(), 0);
        PreparedStatement prep = conn.prepareStatement("insert into s1 values (?);");
        prep.setInt(1, 3);
        assertEquals(prep.executeUpdate(), 1);
        assertNull(prep.getResultSet());
        prep.setInt(1, 5);
        assertEquals(prep.executeUpdate(), 1);
        prep.setInt(1, 7);
        assertEquals(prep.executeUpdate(), 1);
        prep.close();

        // check results with normal statement
        ResultSet rs = stat.executeQuery("select sum(c1) from s1;");
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), 15);
        rs.close();
    }

    @Test
    public void multiUpdate() throws SQLException {
        stat.executeUpdate("create table test (c1);");
        PreparedStatement prep = conn.prepareStatement("insert into test values (?);");

        for (int i = 0; i < 10; i++) {
            prep.setInt(1, i);
            prep.executeUpdate();
            prep.execute();
        }

        prep.close();
        stat.executeUpdate("drop table test;");
    }

    @Test
    public void emptyRS() throws SQLException {
        PreparedStatement prep = conn.prepareStatement("select null limit 0;");
        ResultSet rs = prep.executeQuery();
        assertFalse(rs.next());
        rs.close();
        prep.close();
    }

    @Test
    public void singleRowRS() throws SQLException {
        PreparedStatement prep = conn.prepareStatement("select ?;");
        prep.setInt(1, Integer.MAX_VALUE);
        ResultSet rs = prep.executeQuery();
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), Integer.MAX_VALUE);
        assertEquals(rs.getString(1), Integer.toString(Integer.MAX_VALUE));
        assertEquals(rs.getDouble(1), new Integer(Integer.MAX_VALUE).doubleValue(), 0.0001);
        assertFalse(rs.next());
        rs.close();
        prep.close();
    }

    @Test
    public void twoRowRS() throws SQLException {
        PreparedStatement prep = conn.prepareStatement("select ? union all select ?;");
        prep.setDouble(1, Double.MAX_VALUE);
        prep.setDouble(2, Double.MIN_VALUE);
        ResultSet rs = prep.executeQuery();
        assertTrue(rs.next());
        assertEquals(rs.getDouble(1), Double.MAX_VALUE, 0.0001);
        assertTrue(rs.next());
        assertEquals(rs.getDouble(1), Double.MIN_VALUE, 0.0001);
        assertFalse(rs.next());
        rs.close();
    }

    @Test
    public void stringRS() throws SQLException {
        String name = "Gandhi";
        PreparedStatement prep = conn.prepareStatement("select ?;");
        prep.setString(1, name);
        ResultSet rs = prep.executeQuery();
        assertEquals(-1, prep.getUpdateCount());
        assertTrue(rs.next());
        assertEquals(rs.getString(1), name);
        assertFalse(rs.next());
        rs.close();
    }

    @Test
    public void finalizePrep() throws SQLException {
        conn.prepareStatement("select null;");
        System.gc();
    }

    @Test
    public void set() throws SQLException, UnsupportedEncodingException {
        ResultSet rs;
        PreparedStatement prep = conn.prepareStatement("select ?, ?, ?;");

        // integers
        prep.setInt(1, Integer.MIN_VALUE);
        prep.setInt(2, Integer.MAX_VALUE);
        prep.setInt(3, 0);
        rs = prep.executeQuery();
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), Integer.MIN_VALUE);
        assertEquals(rs.getInt(2), Integer.MAX_VALUE);
        assertEquals(rs.getInt(3), 0);

        // strings
        String name = "Winston Leonard Churchill";
        String fn = name.substring(0, 7), mn = name.substring(8, 15), sn = name.substring(16, 25);
        prep.clearParameters();
        prep.setString(1, fn);
        prep.setString(2, mn);
        prep.setString(3, sn);
        prep.executeQuery();
        assertTrue(rs.next());
        assertEquals(rs.getString(1), fn);
        assertEquals(rs.getString(2), mn);
        assertEquals(rs.getString(3), sn);

        // mixed
        prep.setString(1, name);
        prep.setString(2, null);
        prep.setLong(3, Long.MAX_VALUE);
        prep.executeQuery();
        assertTrue(rs.next());
        assertEquals(rs.getString(1), name);
        assertNull(rs.getString(2));
        assertTrue(rs.wasNull());
        assertEquals(rs.getLong(3), Long.MAX_VALUE);

        // bytes
        prep.setBytes(1, b1);
        prep.setBytes(2, b2);
        prep.setBytes(3, b3);
        prep.executeQuery();
        assertTrue(rs.next());
        assertArrayEq(rs.getBytes(1), b1);
        assertArrayEq(rs.getBytes(2), b2);
        assertArrayEq(rs.getBytes(3), b3);
        assertFalse(rs.next());
        rs.close();

        // streams
        ByteArrayInputStream inByte = new ByteArrayInputStream(b1);
        prep.setBinaryStream(1, inByte, b1.length);
        ByteArrayInputStream inAscii = new ByteArrayInputStream(b2);
        prep.setAsciiStream(2, inAscii, b2.length);
        byte[] b3 = utf08.getBytes("UTF-8");
        ByteArrayInputStream inUnicode = new ByteArrayInputStream(b3);
        prep.setUnicodeStream(3, inUnicode, b3.length);

        rs = prep.executeQuery();
        assertTrue(rs.next());
        assertArrayEq(b1, rs.getBytes(1));
        assertEquals(new String(b2), rs.getString(2));
        assertEquals(new String(b3), rs.getString(3));
        assertFalse(rs.next());
        rs.close();
    }

    @Test
    public void colNameAccess() throws SQLException {
        PreparedStatement prep = conn.prepareStatement("select ? as col1, ? as col2, ? as bingo;");
        prep.setNull(1, 0);
        prep.setFloat(2, Float.MIN_VALUE);
        prep.setShort(3, Short.MIN_VALUE);
        prep.executeQuery();
        ResultSet rs = prep.executeQuery();
        assertTrue(rs.next());
        assertNull(rs.getString("col1"));
        assertTrue(rs.wasNull());
        assertEquals(rs.getFloat("col2"), Float.MIN_VALUE, 0.0001);
        assertEquals(rs.getShort("bingo"), Short.MIN_VALUE);
        rs.close();
        prep.close();
    }

    @Test
    public void insert1000() throws SQLException {
        stat.executeUpdate("create table in1000 (a);");
        PreparedStatement prep = conn.prepareStatement("insert into in1000 values (?);");
        conn.setAutoCommit(false);
        for (int i = 0; i < 1000; i++) {
            prep.setInt(1, i);
            prep.executeUpdate();
        }
        conn.commit();

        ResultSet rs = stat.executeQuery("select count(a) from in1000;");
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), 1000);
        rs.close();
    }

    @Test
    public void getObject() throws SQLException {
        stat.executeUpdate("create table testobj (" + "c1 integer, c2 float, c3, c4 varchar, c5 bit, c6, c7);");
        PreparedStatement prep = conn.prepareStatement("insert into testobj values (?,?,?,?,?,?,?);");

        prep.setInt(1, Integer.MAX_VALUE);
        prep.setFloat(2, Float.MAX_VALUE);
        prep.setDouble(3, Double.MAX_VALUE);
        prep.setLong(4, Long.MAX_VALUE);
        prep.setBoolean(5, false);
        prep.setByte(6, (byte) 7);
        prep.setBytes(7, b1);
        prep.executeUpdate();

        ResultSet rs = stat.executeQuery("select c1,c2,c3,c4,c5,c6,c7 from testobj;");
        assertTrue(rs.next());

        assertEquals(rs.getInt(1), Integer.MAX_VALUE);
        assertEquals((int) rs.getLong(1), Integer.MAX_VALUE);
        assertEquals(rs.getFloat(2), Float.MAX_VALUE, 0f);
        assertEquals(rs.getDouble(3), Double.MAX_VALUE, 0d);
        assertEquals(rs.getLong(4), Long.MAX_VALUE);
        assertFalse(rs.getBoolean(5));
        assertEquals(rs.getByte(6), (byte) 7);
        assertArrayEq(rs.getBytes(7), b1);

        assertNotNull(rs.getObject(1));
        assertNotNull(rs.getObject(2));
        assertNotNull(rs.getObject(3));
        assertNotNull(rs.getObject(4));
        assertNotNull(rs.getObject(5));
        assertNotNull(rs.getObject(6));
        assertNotNull(rs.getObject(7));
        assertTrue(rs.getObject(1) instanceof Integer);
        assertTrue(rs.getObject(2) instanceof Double);
        assertTrue(rs.getObject(3) instanceof Double);
        assertTrue(rs.getObject(4) instanceof String);
        assertTrue(rs.getObject(5) instanceof Integer);
        assertTrue(rs.getObject(6) instanceof Integer);
        assertTrue(rs.getObject(7) instanceof byte[]);
        rs.close();
    }

    @Test
    public void tokens() throws SQLException {
        /* checks for a bug where a substring is read by the driver as the
         * full original string, caused by my idiocyin assuming the
         * pascal-style string was null terminated. Thanks Oliver Randschau. */
        StringTokenizer st = new StringTokenizer("one two three");
        st.nextToken();
        String substr = st.nextToken();

        PreparedStatement prep = conn.prepareStatement("select ?;");
        prep.setString(1, substr);
        ResultSet rs = prep.executeQuery();
        assertTrue(rs.next());
        assertEquals(rs.getString(1), substr);
    }

    @Test
    public void utf() throws SQLException {
        ResultSet rs = stat.executeQuery("select '" + utf01 + "','" + utf02 + "','" + utf03 + "','" + utf04 + "','"
                + utf05 + "','" + utf06 + "','" + utf07 + "','" + utf08 + "';");
        assertEquals(rs.getString(1), utf01);
        assertEquals(rs.getString(2), utf02);
        assertEquals(rs.getString(3), utf03);
        assertEquals(rs.getString(4), utf04);
        assertEquals(rs.getString(5), utf05);
        assertEquals(rs.getString(6), utf06);
        assertEquals(rs.getString(7), utf07);
        assertEquals(rs.getString(8), utf08);
        rs.close();

        PreparedStatement prep = conn.prepareStatement("select ?,?,?,?,?,?,?,?;");
        prep.setString(1, utf01);
        prep.setString(2, utf02);
        prep.setString(3, utf03);
        prep.setString(4, utf04);
        prep.setString(5, utf05);
        prep.setString(6, utf06);
        prep.setString(7, utf07);
        prep.setString(8, utf08);
        rs = prep.executeQuery();
        assertTrue(rs.next());
        assertEquals(rs.getString(1), utf01);
        assertEquals(rs.getString(2), utf02);
        assertEquals(rs.getString(3), utf03);
        assertEquals(rs.getString(4), utf04);
        assertEquals(rs.getString(5), utf05);
        assertEquals(rs.getString(6), utf06);
        assertEquals(rs.getString(7), utf07);
        assertEquals(rs.getString(8), utf08);
        rs.close();
    }

    @Test
    public void batch() throws SQLException {
        ResultSet rs;

        stat.executeUpdate("create table test (c1, c2, c3, c4);");
        PreparedStatement prep = conn.prepareStatement("insert into test values (?,?,?,?);");
        for (int i = 0; i < 10; i++) {
            prep.setInt(1, Integer.MIN_VALUE + i);
            prep.setFloat(2, Float.MIN_VALUE + i);
            prep.setString(3, "Hello " + i);
            prep.setDouble(4, Double.MAX_VALUE + i);
            prep.addBatch();
        }
        assertArrayEq(prep.executeBatch(), new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 });
        prep.close();

        rs = stat.executeQuery("select * from test;");
        for (int i = 0; i < 10; i++) {
            assertTrue(rs.next());
            assertEquals(rs.getInt(1), Integer.MIN_VALUE + i);
            assertEquals(rs.getFloat(2), Float.MIN_VALUE + i, 0.0001);
            assertEquals(rs.getString(3), "Hello " + i);
            assertEquals(rs.getDouble(4), Double.MAX_VALUE + i, 0.0001);
        }
        rs.close();
        stat.executeUpdate("drop table test;");
    }

    @Test
    public void testExecuteBatch() throws Exception {
        stat.executeUpdate("create table t (c text);");
        PreparedStatement prep = conn.prepareStatement("insert into t values (?);");
        prep.setString(1, "a");
        prep.addBatch();
        int call1_length = prep.executeBatch().length;
        prep.setString(1, "b");
        prep.addBatch();
        int call2_length = prep.executeBatch().length;

        assertEquals(1, call1_length);
        assertEquals(1, call2_length);

        ResultSet rs = stat.executeQuery("select * from t");
        rs.next();
        assertEquals("a", rs.getString(1));
        rs.next();
        assertEquals("b", rs.getString(1));
    }

    @Test
    public void dblock() throws SQLException {
        stat.executeUpdate("create table test (c1);");
        stat.executeUpdate("insert into test values (1);");
        conn.prepareStatement("select * from test;").executeQuery().close();
        stat.executeUpdate("drop table test;");

    }

    @Test
    public void dbclose() throws SQLException {
        conn.prepareStatement("select ?;").setString(1, "Hello World");
        conn.prepareStatement("select null;").close();
        conn.prepareStatement("select null;").executeQuery().close();
        conn.prepareStatement("create table t (c);").executeUpdate();
        conn.prepareStatement("select null;");
    }

    @Test
    public void batchOneParam() throws SQLException {
        stat.executeUpdate("create table test (c1);");
        PreparedStatement prep = conn.prepareStatement("insert into test values (?);");
        for (int i = 0; i < 10; i++) {
            prep.setInt(1, Integer.MIN_VALUE + i);
            prep.addBatch();
        }
        assertArrayEq(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }, prep.executeBatch());
        prep.close();
        ResultSet rs = stat.executeQuery("select count(*) from test;");
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), 10);
        rs.close();
    }

    @Test
    public void paramMetaData() throws SQLException {
        PreparedStatement prep = conn.prepareStatement("select ?,?,?,?;");
        assertEquals(prep.getParameterMetaData().getParameterCount(), 4);
    }

    @Test
    public void metaData() throws SQLException {
        PreparedStatement prep = conn.prepareStatement("select ? as col1, ? as col2, ? as delta;");
        ResultSetMetaData meta = prep.getMetaData();
        assertEquals(meta.getColumnCount(), 3);
        assertEquals(meta.getColumnName(1), "col1");
        assertEquals(meta.getColumnName(2), "col2");
        assertEquals(meta.getColumnName(3), "delta");
        /*assertEquals(meta.getColumnType(1), Types.INTEGER);
        assertEquals(meta.getColumnType(2), Types.INTEGER);
        assertEquals(meta.getColumnType(3), Types.INTEGER);*/

        prep.setInt(1, 2);prep.setInt(2, 3);prep.setInt(1, -1);
        meta = prep.executeQuery().getMetaData();
        assertEquals(meta.getColumnCount(), 3);
        prep.close();
    }

    @Test
    public void date1() throws SQLException {
        Date d1 = new Date(987654321);

        stat.execute("create table t (c1);");
        PreparedStatement prep = conn.prepareStatement("insert into t values(?);");
        prep.setDate(1, d1);
        prep.executeUpdate();

        ResultSet rs = stat.executeQuery("select c1 from t;");
        assertTrue(rs.next());
        assertEquals(rs.getLong(1), d1.getTime());
        assertTrue(rs.getDate(1).equals(d1));
        rs.close();
    }

    @Test
    public void date2() throws SQLException {
        Date d1 = new Date(1092941466000L);
        stat.execute("create table t (c1);");
        PreparedStatement prep = conn.prepareStatement("insert into t values (datetime(?/1000, 'unixepoch'));");
        prep.setDate(1, d1);
        prep.executeUpdate();

        ResultSet rs = stat.executeQuery("select strftime('%s', c1) * 1000 from t;");
        assertTrue(rs.next());
        assertEquals(rs.getLong(1), d1.getTime());
        assertTrue(rs.getDate(1).equals(d1));
    }

    @Test
    public void changeSchema() throws SQLException {
        stat.execute("create table t (c1);");
        PreparedStatement prep = conn.prepareStatement("insert into t values (?);");
        conn.createStatement().execute("create table t2 (c2);");
        prep.setInt(1, 1000);
        prep.execute();
        prep.executeUpdate();
    }

    //    @Ignore
    //    @Test
    //    public void multipleStatements() throws SQLException
    //    {
    //        PreparedStatement prep = conn
    //                .prepareStatement("create table person (id integer, name string); insert into person values(1, 'leo'); insert into person values(2, 'yui');");
    //        prep.executeUpdate();
    //
    //        ResultSet rs = conn.createStatement().executeQuery("select * from person");
    //        assertTrue(rs.next());
    //        assertTrue(rs.next());
    //    }

    @Test
    public void reusingSetValues() throws SQLException {
        PreparedStatement prep = conn.prepareStatement("select ?,?;");
        prep.setInt(1, 9);

        for (int i = 0; i < 10; i++) {
            prep.setInt(2, i);
            ResultSet rs = prep.executeQuery();
            assertTrue(rs.next());
            assertEquals(rs.getInt(1), 9);
            assertEquals(rs.getInt(2), i);
        }

        for (int i = 0; i < 10; i++) {
            prep.setInt(2, i);
            ResultSet rs = prep.executeQuery();
            assertTrue(rs.next());
            assertEquals(rs.getInt(1), 9);
            assertEquals(rs.getInt(2), i);
            rs.close();
        }

        prep.close();
    }

    @Test
    public void clearParameters() throws SQLException {
        stat.executeUpdate("create table tbl (colid integer primary key AUTOINCREMENT, col varchar)");
        stat.executeUpdate("insert into tbl(col) values (\"foo\")");

        PreparedStatement prep = conn.prepareStatement("select colid from tbl where col = ?");

        prep.setString(1, "foo");

        ResultSet rs = prep.executeQuery();
        prep.clearParameters();
        rs.next();

        assertEquals(1, rs.getInt(1));

        rs.close();

        try {
            prep.execute();
            fail("Returned result when values not bound to prepared statement");
        } catch (Exception e) {
            assertEquals("Values not bound to statement", e.getMessage());
        }

        try {
            rs = prep.executeQuery();
            fail("Returned result when values not bound to prepared statement");
        } catch (Exception e) {
            assertEquals("Values not bound to statement", e.getMessage());
        }

        prep.close();

        try {
            prep = conn.prepareStatement("insert into tbl(col) values (?)");
            prep.clearParameters();
            prep.executeUpdate();
            fail("Returned result when values not bound to prepared statement");
        } catch (Exception e) {
            assertEquals("Values not bound to statement", e.getMessage());
        }
    }

    @Test(expected = SQLException.class)
    public void noSuchTable() throws SQLException {
        PreparedStatement prep = conn.prepareStatement("select * from doesnotexist;");
        prep.executeQuery();
    }

    @Test(expected = SQLException.class)
    public void noSuchCol() throws SQLException {
        PreparedStatement prep = conn.prepareStatement("select notacol from (select 1);");
        prep.executeQuery();
    }

    @Test(expected = SQLException.class)
    public void noSuchColName() throws SQLException {
        ResultSet rs = conn.prepareStatement("select 1;").executeQuery();
        assertTrue(rs.next());
        rs.getInt("noSuchColName");
    }

    private void assertArrayEq(byte[] a, byte[] b) {
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++)
            assertEquals(a[i], b[i]);
    }

    private void assertArrayEq(int[] a, int[] b) {
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++)
            assertEquals(a[i], b[i]);
    }
}
