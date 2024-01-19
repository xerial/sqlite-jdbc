package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.StringTokenizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** These tests are designed to stress PreparedStatements on memory dbs. */
public class PrepStmtTest {
    static byte[] b1 = new byte[] {1, 2, 7, 4, 2, 6, 2, 8, 5, 2, 3, 1, 5, 3, 6, 3, 3, 6, 2, 5};
    static byte[] b2 = getUtf8Bytes("To be or not to be.");
    static byte[] b3 = getUtf8Bytes("Question!#$%");
    static String utf01 = "\uD840\uDC40";
    static String utf02 = "\uD840\uDC47 ";
    static String utf03 = " \uD840\uDC43";
    static String utf04 = " \uD840\uDC42 ";
    static String utf05 = "\uD840\uDC40\uD840\uDC44";
    static String utf06 = "Hello World, \uD840\uDC40 \uD880\uDC99";
    static String utf07 = "\uD840\uDC41 testing \uD880\uDC99";
    static String utf08 = "\uD840\uDC40\uD840\uDC44 testing";

    private Connection conn;
    private Statement stat;

    private static byte[] getUtf8Bytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    @BeforeEach
    public void connect() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
    }

    @AfterEach
    public void close() throws SQLException {
        stat.close();
        conn.close();
    }

    @Test
    public void update() throws SQLException {
        assertThat(conn.prepareStatement("create table s1 (c1);").executeUpdate()).isEqualTo(0);
        PreparedStatement prep = conn.prepareStatement("insert into s1 values (?);");
        prep.setInt(1, 3);
        assertThat(prep.executeUpdate()).isEqualTo(1);
        assertThat(prep.getResultSet()).isNull();
        prep.setInt(1, 5);
        assertThat(prep.executeUpdate()).isEqualTo(1);
        prep.setInt(1, 7);
        assertThat(prep.executeUpdate()).isEqualTo(1);

        ResultSet rsgk = prep.getGeneratedKeys();
        assertThat(rsgk.next()).isTrue();
        assertThat(rsgk.getInt(1)).isEqualTo(3);
        rsgk.close();

        prep.close();
        // check results with normal statement
        ResultSet rs = stat.executeQuery("select sum(c1) from s1;");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(15);
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
        assertThat(rs.next()).isFalse();
        rs.close();
        prep.close();
    }

    @Test
    public void singleRowRS() throws SQLException {
        PreparedStatement prep = conn.prepareStatement("select ?;");
        prep.setInt(1, Integer.MAX_VALUE);
        ResultSet rs = prep.executeQuery();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(Integer.MAX_VALUE);
        assertThat(rs.getString(1)).isEqualTo(Integer.toString(Integer.MAX_VALUE));
        assertThat(rs.getDouble(1))
                .isCloseTo(new Integer(Integer.MAX_VALUE).doubleValue(), offset(0.0001));
        assertThat(rs.next()).isFalse();
        rs.close();
        prep.close();
    }

    @Test
    public void twoRowRS() throws SQLException {
        PreparedStatement prep = conn.prepareStatement("select ? union all select ?;");
        prep.setDouble(1, Double.MAX_VALUE);
        prep.setDouble(2, Double.MIN_VALUE);
        ResultSet rs = prep.executeQuery();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(Double.MAX_VALUE, offset(0.0001));
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(Double.MIN_VALUE, offset(0.0001));
        assertThat(rs.next()).isFalse();
        rs.close();
    }

    @Test
    public void stringRS() throws SQLException {
        String name = "Gandhi";
        PreparedStatement prep = conn.prepareStatement("select ?;");
        prep.setString(1, name);
        ResultSet rs = prep.executeQuery();
        assertThat(prep.getUpdateCount()).isEqualTo(-1);
        assertThat(rs.next()).isTrue();
        assertThat(name).isEqualTo(rs.getString(1));
        assertThat(rs.next()).isFalse();
        rs.close();
    }

    @Test
    public void finalizePrep() throws SQLException {
        conn.prepareStatement("select null;");
        System.gc();
    }

    @Test
    public void set() throws SQLException {
        ResultSet rs;
        PreparedStatement prep = conn.prepareStatement("select ?, ?, ?;");

        // integers
        prep.setInt(1, Integer.MIN_VALUE);
        prep.setInt(2, Integer.MAX_VALUE);
        prep.setInt(3, 0);
        rs = prep.executeQuery();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(Integer.MIN_VALUE);
        assertThat(rs.getInt(2)).isEqualTo(Integer.MAX_VALUE);
        assertThat(rs.getInt(3)).isEqualTo(0);

        // strings
        String name = "Winston Leonard Churchill";
        String fn = name.substring(0, 7), mn = name.substring(8, 15), sn = name.substring(16, 25);
        prep.clearParameters();
        prep.setString(1, fn);
        prep.setString(2, mn);
        prep.setString(3, sn);
        prep.executeQuery();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(1)).isEqualTo(fn);
        assertThat(rs.getString(2)).isEqualTo(mn);
        assertThat(rs.getString(3)).isEqualTo(sn);

        // mixed
        prep.setString(1, name);
        prep.setString(2, null);
        prep.setLong(3, Long.MAX_VALUE);
        prep.executeQuery();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(1)).isEqualTo(name);
        assertThat(rs.getString(2)).isNull();
        assertThat(rs.wasNull()).isTrue();
        assertThat(rs.getLong(3)).isEqualTo(Long.MAX_VALUE);

        // bytes
        prep.setBytes(1, b1);
        prep.setBytes(2, b2);
        prep.setBytes(3, b3);
        prep.executeQuery();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getBytes(1)).containsExactly(b1);
        assertThat(rs.getBytes(1)).containsExactly(b1);
        assertThat(rs.getBytes(2)).containsExactly(b2);
        assertThat(rs.getBytes(3)).containsExactly(b3);
        assertThat(rs.next()).isFalse();
        rs.close();

        // null date, time and timestamp (fix #363)
        prep.setDate(1, null);
        prep.setTime(2, null);
        prep.setTimestamp(3, null);
        rs = prep.executeQuery();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDate(1)).isNull();
        assertThat(rs.getTime(2)).isNull();
        assertThat(rs.getTimestamp(3)).isNull();

        // streams
        ByteArrayInputStream inByte = new ByteArrayInputStream(b1);
        prep.setBinaryStream(1, inByte, b1.length);
        ByteArrayInputStream inAscii = new ByteArrayInputStream(b2);
        prep.setAsciiStream(2, inAscii, b2.length);
        byte[] b3 = utf08.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream inUnicode = new ByteArrayInputStream(b3);
        prep.setUnicodeStream(3, inUnicode, b3.length);

        rs = prep.executeQuery();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getBytes(1)).containsExactly(b1);
        assertThat(rs.getString(2)).isEqualTo(new String(b2, StandardCharsets.UTF_8));
        assertThat(rs.getString(3)).isEqualTo(new String(b3, StandardCharsets.UTF_8));
        assertThat(rs.next()).isFalse();
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
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("col1")).isNull();
        assertThat(rs.wasNull()).isTrue();
        assertThat(rs.getFloat("col2")).isCloseTo(Float.MIN_VALUE, offset(0.0001F));
        assertThat(rs.getShort("bingo")).isEqualTo(Short.MIN_VALUE);
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
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(1000);
        rs.close();
    }

    @Test
    public void getObject() throws SQLException {
        stat.executeUpdate(
                "create table testobj ("
                        + "c1 integer, c2 float, c3, c4 varchar, c5 bit, c6, c7);");
        PreparedStatement prep =
                conn.prepareStatement("insert into testobj values (?,?,?,?,?,?,?);");

        prep.setInt(1, Integer.MAX_VALUE);
        prep.setFloat(2, Float.MAX_VALUE);
        prep.setDouble(3, Double.MAX_VALUE);
        prep.setLong(4, Long.MAX_VALUE);
        prep.setBoolean(5, false);
        prep.setByte(6, (byte) 7);
        prep.setBytes(7, b1);
        prep.executeUpdate();

        ResultSet rs = stat.executeQuery("select c1,c2,c3,c4,c5,c6,c7 from testobj;");
        assertThat(rs.next()).isTrue();

        assertThat(rs.getInt(1)).isEqualTo(Integer.MAX_VALUE);
        assertThat((int) rs.getLong(1)).isEqualTo(Integer.MAX_VALUE);
        assertThat(rs.getFloat(2)).isCloseTo(Float.MAX_VALUE, offset(0.0001f));
        assertThat(rs.getDouble(3)).isCloseTo(Double.MAX_VALUE, offset(0.0001d));
        assertThat(rs.getLong(4)).isEqualTo(Long.MAX_VALUE);
        assertThat(rs.getBoolean(5)).isFalse();
        assertThat(rs.getByte(6)).isEqualTo((byte) 7);
        assertThat(rs.getBytes(7)).containsExactly(b1);

        assertThat(rs.getObject(1)).isNotNull();
        assertThat(rs.getObject(2)).isNotNull();
        assertThat(rs.getObject(3)).isNotNull();
        assertThat(rs.getObject(4)).isNotNull();
        assertThat(rs.getObject(5)).isNotNull();
        assertThat(rs.getObject(6)).isNotNull();
        assertThat(rs.getObject(7)).isNotNull();
        assertThat(rs.getObject(1) instanceof Integer).isTrue();
        assertThat(rs.getObject(2) instanceof Double).isTrue();
        assertThat(rs.getObject(3) instanceof Double).isTrue();
        assertThat(rs.getObject(4) instanceof String).isTrue();
        assertThat(rs.getObject(5) instanceof Integer).isTrue();
        assertThat(rs.getObject(6) instanceof Integer).isTrue();
        assertThat(rs.getObject(7) instanceof byte[]).isTrue();
        rs.close();
    }

    @Test
    public void tokens() throws SQLException {
        /* checks for a bug where a substring is read by the driver as the
         * full original string, caused by my idiocy in assuming the
         * pascal-style string was null terminated. Thanks Oliver Randschau. */
        StringTokenizer st = new StringTokenizer("one two three");
        st.nextToken();
        String substr = st.nextToken();

        PreparedStatement prep = conn.prepareStatement("select ?;");
        prep.setString(1, substr);
        ResultSet rs = prep.executeQuery();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(1)).isEqualTo(substr);
    }

    @Test
    public void utf() throws SQLException {
        ResultSet rs =
                stat.executeQuery(
                        "select '"
                                + utf01
                                + "','"
                                + utf02
                                + "','"
                                + utf03
                                + "','"
                                + utf04
                                + "','"
                                + utf05
                                + "','"
                                + utf06
                                + "','"
                                + utf07
                                + "','"
                                + utf08
                                + "';");
        assertThat(rs.getBytes(1)).containsExactly(getUtf8Bytes(utf01));
        assertThat(rs.getBytes(2)).containsExactly(getUtf8Bytes(utf02));
        assertThat(rs.getBytes(3)).containsExactly(getUtf8Bytes(utf03));
        assertThat(rs.getBytes(4)).containsExactly(getUtf8Bytes(utf04));
        assertThat(rs.getBytes(5)).containsExactly(getUtf8Bytes(utf05));
        assertThat(rs.getBytes(6)).containsExactly(getUtf8Bytes(utf06));
        assertThat(rs.getBytes(7)).containsExactly(getUtf8Bytes(utf07));
        assertThat(rs.getBytes(8)).containsExactly(getUtf8Bytes(utf08));
        assertThat(rs.getString(1)).isEqualTo(utf01);
        assertThat(rs.getString(2)).isEqualTo(utf02);
        assertThat(rs.getString(3)).isEqualTo(utf03);
        assertThat(rs.getString(4)).isEqualTo(utf04);
        assertThat(rs.getString(5)).isEqualTo(utf05);
        assertThat(rs.getString(6)).isEqualTo(utf06);
        assertThat(rs.getString(7)).isEqualTo(utf07);
        assertThat(rs.getString(8)).isEqualTo(utf08);
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
        assertThat(rs.next()).isTrue();
        assertThat(rs.getBytes(1)).containsExactly(getUtf8Bytes(utf01));
        assertThat(rs.getBytes(2)).containsExactly(getUtf8Bytes(utf02));
        assertThat(rs.getBytes(3)).containsExactly(getUtf8Bytes(utf03));
        assertThat(rs.getBytes(4)).containsExactly(getUtf8Bytes(utf04));
        assertThat(rs.getBytes(5)).containsExactly(getUtf8Bytes(utf05));
        assertThat(rs.getBytes(6)).containsExactly(getUtf8Bytes(utf06));
        assertThat(rs.getBytes(7)).containsExactly(getUtf8Bytes(utf07));
        assertThat(rs.getBytes(8)).containsExactly(getUtf8Bytes(utf08));
        assertThat(rs.getString(1)).isEqualTo(utf01);
        assertThat(rs.getString(2)).isEqualTo(utf02);
        assertThat(rs.getString(3)).isEqualTo(utf03);
        assertThat(rs.getString(4)).isEqualTo(utf04);
        assertThat(rs.getString(5)).isEqualTo(utf05);
        assertThat(rs.getString(6)).isEqualTo(utf06);
        assertThat(rs.getString(7)).isEqualTo(utf07);
        assertThat(rs.getString(8)).isEqualTo(utf08);
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
        assertThat(prep.executeBatch()).containsExactly(1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
        prep.close();

        rs = stat.executeQuery("select * from test;");
        for (int i = 0; i < 10; i++) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(Integer.MIN_VALUE + i);
            assertThat(rs.getFloat(2)).isCloseTo(Float.MIN_VALUE + i, offset(0.0001F));
            assertThat(rs.getString(3)).isEqualTo("Hello " + i);
            assertThat(rs.getDouble(4)).isCloseTo(Double.MAX_VALUE + i, offset(0.0001));
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

        assertThat(call1_length).isEqualTo(1);
        assertThat(call2_length).isEqualTo(1);

        ResultSet rs = stat.executeQuery("select * from t");
        rs.next();
        assertThat(rs.getString(1)).isEqualTo("a");
        rs.next();
        assertThat(rs.getString(1)).isEqualTo("b");
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
        assertThat(new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}).containsExactly(prep.executeBatch());
        prep.close();
        ResultSet rs = stat.executeQuery("select count(*) from test;");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(10);
        rs.close();
    }

    @Test
    public void batchZeroParams() throws Exception {
        stat.executeUpdate("create table test (c1);");
        PreparedStatement prep = conn.prepareStatement("insert into test values (5);");
        for (int i = 0; i < 10; i++) {
            prep.addBatch();
        }
        assertThat(new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}).containsExactly(prep.executeBatch());
        prep.close();
        ResultSet rs = stat.executeQuery("select count(*) from test;");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(10);
        rs.close();
    }

    @Test
    public void paramMetaData() throws SQLException {
        PreparedStatement prep = conn.prepareStatement("select ?,?,?,?;");
        assertThat(prep.getParameterMetaData().getParameterCount()).isEqualTo(4);
    }

    @Test
    public void metaData() throws SQLException {
        PreparedStatement prep = conn.prepareStatement("select ? as col1, ? as col2, ? as delta;");
        ResultSetMetaData meta = prep.getMetaData();
        assertThat(meta.getColumnCount()).isEqualTo(3);
        assertThat(meta.getColumnName(1)).isEqualTo("col1");
        assertThat(meta.getColumnName(2)).isEqualTo("col2");
        assertThat(meta.getColumnName(3)).isEqualTo("delta");
        assertThat(meta.getColumnType(1)).isEqualTo(Types.NUMERIC);
        assertThat(meta.getColumnType(2)).isEqualTo(Types.NUMERIC);
        assertThat(meta.getColumnType(3)).isEqualTo(Types.NUMERIC);

        prep.setInt(1, 2);
        prep.setInt(2, 3);
        prep.setInt(3, -1);
        meta = prep.executeQuery().getMetaData();
        assertThat(meta.getColumnCount()).isEqualTo(3);
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
        assertThat(rs.next()).isTrue();
        assertThat(rs.getLong(1)).isEqualTo(d1.getTime());
        assertThat(rs.getDate(1)).isEqualTo(d1);
        rs.close();
    }

    @Test
    public void date2() throws SQLException {
        Date d1 = new Date(1092941466000L);
        stat.execute("create table t (c1);");
        PreparedStatement prep =
                conn.prepareStatement("insert into t values (datetime(?/1000, 'unixepoch'));");
        prep.setDate(1, d1);
        prep.executeUpdate();

        ResultSet rs = stat.executeQuery("select strftime('%s', c1) * 1000 from t;");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getLong(1)).isEqualTo(d1.getTime());
        assertThat(rs.getDate(1)).isEqualTo(d1);
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
    //                .prepareStatement("create table person (id integer, name string); insert into
    // person values(1, 'leo'); insert into person values(2, 'yui');");
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
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(9);
            assertThat(rs.getInt(2)).isEqualTo(i);
        }

        for (int i = 0; i < 10; i++) {
            prep.setInt(2, i);
            ResultSet rs = prep.executeQuery();
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(9);
            assertThat(rs.getInt(2)).isEqualTo(i);
            rs.close();
        }

        prep.close();
    }

    @Test
    public void clearParameters() throws SQLException {
        stat.executeUpdate(
                "create table tbl (colid integer primary key AUTOINCREMENT, col varchar)");
        stat.executeUpdate("insert into tbl(col) values (\"foo\")");
        stat.executeUpdate("insert into tbl(col) values (?)");

        PreparedStatement prep = conn.prepareStatement("select colid from tbl where col = ?");

        prep.setString(1, "foo");

        ResultSet rs = prep.executeQuery();
        prep.clearParameters();
        rs.next();

        assertThat(rs.getInt(1)).isEqualTo(1);

        rs.close();

        // should not throw
        prep.execute();

        // should not throw
        PreparedStatement nullPrep =
                conn.prepareStatement("select colid from tbl where col is null");
        rs = nullPrep.executeQuery();
        rs.next();

        // gets the row with the NULL column
        assertThat(rs.getInt(1)).isEqualTo(2);

        rs.close();
        nullPrep.close();
    }

    @Test
    public void preparedStatementShouldNotThrowIfNotAllParamsSet() throws SQLException {
        PreparedStatement prep = conn.prepareStatement("select ? as col1, ? as col2, ? as col3;");
        ResultSetMetaData meta = prep.getMetaData();

        // leaves 0 and 1 unbound
        assertThat(meta.getColumnCount()).isEqualTo(3);

        // we only set one 1 param of the expected 3 params
        prep.setInt(1, 2);
        prep.executeQuery();
        prep.close();
    }

    @Test
    public void preparedStatementShouldNotThrowIfNotAllParamsSetBatch() throws SQLException {
        stat.executeUpdate("create table test (c1, c2);");
        PreparedStatement prep = conn.prepareStatement("insert into test values (?,?);");

        // leaves param 0 unbound
        prep.setInt(1, 1);

        prep.addBatch();
    }

    @Test
    public void noSuchTable() {
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> conn.prepareStatement("select * from doesnotexist;"));
    }

    @Test
    public void noSuchCol() {
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> conn.prepareStatement("select notacol from (select 1);"));
    }

    @Test
    public void noSuchColName() throws SQLException {
        ResultSet rs = conn.prepareStatement("select 1;").executeQuery();
        assertThat(rs.next()).isTrue();
        assertThatExceptionOfType(SQLException.class).isThrownBy(() -> rs.getInt("noSuchColName"));
    }

    @Test
    public void constraintErrorCodeExecute() throws SQLException {
        assertThat(
                        stat.executeUpdate(
                                "create table foo (id integer, CONSTRAINT U_ID UNIQUE (id));"))
                .isEqualTo(0);
        assertThat(stat.executeUpdate("insert into foo values(1);")).isEqualTo(1);
        // try to insert a row with duplicate id
        try (PreparedStatement statement = conn.prepareStatement("insert into foo values(?);")) {
            statement.setInt(1, 1);

            assertThatThrownBy(statement::execute)
                    .isInstanceOfSatisfying(
                            SQLiteException.class,
                            (e) -> {
                                assertThat(e.getErrorCode())
                                        .isEqualTo(SQLiteErrorCode.SQLITE_CONSTRAINT.code);
                                assertThat(e.getResultCode())
                                        .isEqualTo(SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE);
                            });
        }
    }

    @Test
    public void constraintErrorCodeExecuteUpdate() throws SQLException {
        assertThat(
                        stat.executeUpdate(
                                "create table foo (id integer, CONSTRAINT U_ID UNIQUE (id));"))
                .isEqualTo(0);
        assertThat(stat.executeUpdate("insert into foo values(1);")).isEqualTo(1);
        // try to insert a row with duplicate id
        try (PreparedStatement statement = conn.prepareStatement("insert into foo values(?);")) {
            statement.setInt(1, 1);
            assertThatThrownBy(statement::executeUpdate)
                    .isInstanceOfSatisfying(
                            SQLiteException.class,
                            (e) -> {
                                assertThat(e.getErrorCode())
                                        .isEqualTo(SQLiteErrorCode.SQLITE_CONSTRAINT.code);
                                assertThat(e.getResultCode())
                                        .isEqualTo(SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE);
                            });
        }
    }

    @Test
    public void getMoreResultsDoesNotCloseStatement() throws SQLException {
        PreparedStatement ps = conn.prepareStatement("select ?");
        ps.setString(1, "Hello");

        ResultSet rs = ps.executeQuery();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(1)).isEqualTo("Hello");
        assertThat(rs.next()).isFalse();

        assertThat(ps.getMoreResults()).isFalse();
        assertThat(rs.isClosed()).isTrue();
        assertThat(ps.isClosed()).isFalse();

        assertThatNoException().isThrownBy(ps::clearParameters);
    }

    @Test
    public void gh810_getMoreResults_and_getUpdateCount() throws SQLException {
        stat.executeUpdate("create table t(i int)");

        PreparedStatement ps = conn.prepareStatement("update t set i = 0 where false");
        assertThat(ps.execute()).isFalse();
        assertThat(ps.getUpdateCount()).isEqualTo(0);
        assertThat(ps.getMoreResults()).isFalse();
        assertThat(ps.getUpdateCount()).isEqualTo(-1);
    }

    @Test
    public void executeUpdateCount() throws SQLException {
        PreparedStatement ps1 = conn.prepareStatement("create table test (c1)");
        assertThat(ps1.execute()).isFalse();

        PreparedStatement ps2 = conn.prepareStatement("insert into test values('abc'),('def')");
        assertThat(ps2.execute()).isFalse();
        assertThat(ps2.getUpdateCount()).isEqualTo(2);
        assertThat(ps2.getMoreResults()).isFalse();
        assertThat(ps2.getUpdateCount()).isEqualTo(-1);

        assertThat(ps1.getUpdateCount()).isEqualTo(0);
        assertThat(ps1.getMoreResults()).isFalse();
        assertThat(ps1.getUpdateCount()).isEqualTo(-1);
    }

    @Test
    public void gh811_getMetadata_before_execution() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("select 1")) {
            ps.executeQuery();
            ResultSetMetaData meta = ps.getMetaData();
            assertThat(meta).isNotNull();
            assertThat(meta.getColumnCount()).isEqualTo(1);
            assertThat(meta.getColumnClassName(1)).isEqualTo("java.lang.Integer");
        }

        try (PreparedStatement ps = conn.prepareStatement("select 1")) {
            ResultSetMetaData meta = ps.getMetaData();
            assertThat(meta).isNotNull();
            assertThat(meta.getColumnCount()).isEqualTo(1);
            assertThat(meta.getColumnClassName(1)).isEqualTo("java.lang.Object");
        }
    }

    @Test
    public void getParameterTypeTest() throws SQLException {
        stat.executeUpdate("create table t_int(i INT)");

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO t_int VALUES(?)")) {
            ps.setLong(1, 100);
            assertThat(ps.getParameterMetaData().getParameterType(1)).isEqualTo(Types.BIGINT);
            assertThat(ps.getParameterMetaData().getParameterTypeName(1)).isEqualTo("BIGINT");
        }

        stat.executeUpdate("create table t_real(a REAL, b REAL)");

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO t_real VALUES(?, ?)")) {
            ps.setDouble(1, 100.0);
            ps.setFloat(2, 100.0f);
            assertThat(ps.getParameterMetaData().getParameterType(1)).isEqualTo(Types.REAL);
            assertThat(ps.getParameterMetaData().getParameterTypeName(1)).isEqualTo("REAL");
            assertThat(ps.getParameterMetaData().getParameterType(2)).isEqualTo(Types.REAL);
            assertThat(ps.getParameterMetaData().getParameterTypeName(2)).isEqualTo("REAL");
        }
    }

    @Test
    void getParameterTypeTest_when_no_parameter_set() throws SQLException {
        stat.executeUpdate("create table t_int(i INT)");

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO t_int VALUES(?)")) {
            assertThatThrownBy(() -> ps.getParameterMetaData().getParameterType(1))
                    .isInstanceOf(SQLException.class)
                    .hasMessage("No parameter has been set yet");
            assertThatThrownBy(() -> ps.getParameterMetaData().getParameterTypeName(1))
                    .isInstanceOf(SQLException.class)
                    .hasMessage("No parameter has been set yet");
        }
    }

    @Test
    public void gh914_reuseExecute() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
            assertThat(ps.execute()).isTrue();
            ResultSet rs = ps.getResultSet();
            assertThat(rs.next()).isTrue();
            assertThat(rs.next()).isFalse();
            assertThat(ps.getMoreResults()).isFalse();

            ResultSet rs2 = ps.executeQuery();
            assertThat(rs2).isNotNull();
        }
    }

    @Test
    public void gh1002_pi() throws SQLException {
        BigDecimal pi = new BigDecimal("3.14");
        stat.executeUpdate("create table gh1002(nr number(10,2))");

        try (PreparedStatement ps = conn.prepareStatement("insert into gh1002 values (?)")) {
            ps.setBigDecimal(1, pi);
            ps.execute();
        }

        ResultSet rs = stat.executeQuery("select nr from gh1002");
        assertThat(rs.getBigDecimal(1)).isEqualTo(pi);
    }

    @Test
    public void gh1002_pi_real() throws SQLException {
        BigDecimal pi = new BigDecimal("3.14");
        stat.executeUpdate("create table gh1002(nr REAL)");

        try (PreparedStatement ps = conn.prepareStatement("insert into gh1002 values (?)")) {
            ps.setBigDecimal(1, pi);
            ps.execute();
        }

        ResultSet rs = stat.executeQuery("select nr from gh1002");
        assertThat(rs.getBigDecimal(1)).isEqualTo(pi);
    }

    @Test
    public void gh1002_pi_text() throws SQLException {
        BigDecimal pi = new BigDecimal("3.14");
        stat.executeUpdate("create table gh1002(nr TEXT)");

        try (PreparedStatement ps = conn.prepareStatement("insert into gh1002 values (?)")) {
            ps.setBigDecimal(1, pi);
            ps.execute();
        }

        ResultSet rs = stat.executeQuery("select nr from gh1002");
        assertThat(rs.getBigDecimal(1)).isEqualTo(pi);
    }
}
