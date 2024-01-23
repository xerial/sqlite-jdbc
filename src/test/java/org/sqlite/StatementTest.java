package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.data.Offset.offset;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Calendar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sqlite.jdbc3.JDBC3Statement;
import org.sqlite.jdbc4.JDBC4Statement;

/** These tests are designed to stress Statements on memory databases. */
public class StatementTest {
    private Connection conn;
    private Statement stat;

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
    public void executeUpdate() throws SQLException {
        assertThat(stat.executeUpdate("create table s1 (c1);")).isEqualTo(0);
        assertThat(stat.executeUpdate("insert into s1 values (0);")).isEqualTo(1);
        assertThat(stat.executeUpdate("insert into s1 values (1);")).isEqualTo(1);
        assertThat(stat.executeUpdate("insert into s1 values (2);")).isEqualTo(1);
        assertThat(stat.executeUpdate("update s1 set c1 = 5;")).isEqualTo(3);
        // count_changes_pgrama. truncate_optimization
        assertThat(stat.executeUpdate("delete from s1;")).isEqualTo(3);

        // multiple SQL statements
        assertThat(stat.executeUpdate("insert into s1 values (11);" + "insert into s1 values (12)"))
                .isEqualTo(2);
        assertThat(
                        stat.executeUpdate(
                                "update s1 set c1 = 21 where c1 = 11;"
                                        + "update s1 set c1 = 22 where c1 = 12;"
                                        + "update s1 set c1 = 23 where c1 = 13"))
                .isEqualTo(2); // c1 = 13 does not exist
        assertThat(
                        stat.executeUpdate(
                                "delete from s1 where c1 = 21;"
                                        + "delete from s1 where c1 = 22;"
                                        + "delete from s1 where c1 = 23"))
                .isEqualTo(2); // c1 = 23 does not exist

        assertThat(stat.executeUpdate("drop table s1;")).isEqualTo(0);
    }

    @Test
    public void emptyRS() throws SQLException {
        ResultSet rs = stat.executeQuery("select null limit 0;");
        assertThat(rs.next()).isFalse();
        rs.close();
    }

    @Test
    public void singleRowRS() throws SQLException {
        ResultSet rs = stat.executeQuery("select " + Integer.MAX_VALUE + ";");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(Integer.MAX_VALUE);
        assertThat(rs.getString(1)).isEqualTo(Integer.toString(Integer.MAX_VALUE));
        assertThat(0.001)
                .isCloseTo(new Integer(Integer.MAX_VALUE).doubleValue(), offset(rs.getDouble(1)));
        assertThat(rs.next()).isFalse();
        rs.close();
    }

    @Test
    public void twoRowRS() throws SQLException {
        ResultSet rs = stat.executeQuery("select 9 union all select 7;");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(9);
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(7);
        assertThat(rs.next()).isFalse();
        rs.close();
    }

    @Test
    public void autoClose() throws SQLException {
        conn.createStatement().executeQuery("select 1;");
    }

    @Test
    public void stringRS() throws SQLException {
        ResultSet rs = stat.executeQuery("select \"Russell\";");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(1)).isEqualTo("Russell");
        assertThat(rs.next()).isFalse();
        rs.close();
    }

    @Test
    public void execute() throws SQLException {
        assertThat(stat.execute("select null;")).isTrue();
        ResultSet rs = stat.getResultSet();
        assertThat(rs).isNotNull();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(1)).isNull();
        assertThat(rs.wasNull()).isTrue();
        assertThat(stat.getMoreResults()).isFalse();
        assertThat(stat.getUpdateCount()).isEqualTo(-1);
        assertThat(stat.isClosed()).isFalse();
        assertThat(stat.getResultSet()).isNull();

        assertThat(stat.execute("select null;")).isTrue();
        assertThat(stat.getMoreResults()).isFalse();
        assertThat(stat.getUpdateCount()).isEqualTo(-1);
        assertThat(stat.isClosed()).isFalse();
        assertThat(stat.getResultSet()).isNull();

        assertThat(stat.execute("create table test (c1);")).isFalse();
        assertThat(stat.getUpdateCount()).isEqualTo(0);
        assertThat(stat.getMoreResults()).isFalse();
        assertThat(stat.getUpdateCount()).isEqualTo(-1);
        assertThat(stat.isClosed()).isFalse();
        assertThat(stat.getResultSet()).isNull();
    }

    @Test
    public void gh_809_execute_reuseStatement() throws SQLException {
        for (int i = 0; i < 2; i++) {
            assertThat(stat.execute("select 1")).isTrue();

            try (ResultSet rs = stat.getResultSet()) {
                assertThat(rs).isNotNull();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
                assertThat(rs.next()).isFalse();
            }

            assertThat(stat.getMoreResults()).isFalse();
            assertThat(stat.getUpdateCount()).isEqualTo(-1);
        }
    }

    @Test
    public void gh_809_executeQuery_reuseStatement() throws SQLException {
        for (int i = 0; i < 2; i++) {
            ResultSet rs = stat.executeQuery("select 1");

            assertThat(rs).isNotNull();
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1);
            assertThat(rs.next()).isFalse();

            assertThat(stat.getMoreResults()).isFalse();
            assertThat(stat.getUpdateCount()).isEqualTo(-1);
        }
    }

    @Test
    public void executeUpdateCount() throws SQLException {
        assertThat(stat.execute("create table test (c1);")).isFalse();

        Statement stat2 = conn.createStatement();
        assertThat(stat2.execute("insert into test values('abc'),('def');")).isFalse();
        assertThat(stat2.getUpdateCount()).isEqualTo(2);
        assertThat(stat2.getMoreResults()).isFalse();
        assertThat(stat2.getUpdateCount()).isEqualTo(-1);

        assertThat(stat.getUpdateCount()).isEqualTo(0);
        assertThat(stat.getMoreResults()).isFalse();
        assertThat(stat.getUpdateCount()).isEqualTo(-1);
    }

    @Test
    public void colNameAccess() throws SQLException {
        assertThat(stat.executeUpdate("create table tab (id, firstname, surname);")).isEqualTo(0);
        assertThat(stat.executeUpdate("insert into tab values (0, 'Bob', 'Builder');"))
                .isEqualTo(1);
        assertThat(stat.executeUpdate("insert into tab values (1, 'Fred', 'Blogs');")).isEqualTo(1);
        assertThat(stat.executeUpdate("insert into tab values (2, 'John', 'Smith');")).isEqualTo(1);
        ResultSet rs = stat.executeQuery("select * from tab;");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt("id")).isEqualTo(0);
        assertThat(rs.getString("firstname")).isEqualTo("Bob");
        assertThat(rs.getString("surname")).isEqualTo("Builder");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt("id")).isEqualTo(1);
        assertThat(rs.getString("firstname")).isEqualTo("Fred");
        assertThat(rs.getString("surname")).isEqualTo("Blogs");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt("id")).isEqualTo(2);
        assertThat(rs.getString("id")).isEqualTo("2");
        assertThat(rs.getString("firstname")).isEqualTo("John");
        assertThat(rs.getString("surname")).isEqualTo("Smith");
        assertThat(rs.next()).isFalse();
        rs.close();
        assertThat(stat.executeUpdate("drop table tab;")).isEqualTo(0);
    }

    @Test
    public void nulls() throws SQLException {
        ResultSet rs = stat.executeQuery("select null union all select null;");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(1)).isNull();
        assertThat(rs.wasNull()).isTrue();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(1)).isNull();
        assertThat(rs.wasNull()).isTrue();
        assertThat(rs.next()).isFalse();
        rs.close();
    }

    @Test
    public void nullsForGetObject() throws SQLException {
        ResultSet rs = stat.executeQuery("select 1, null union all select null, null;");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(1)).isNotNull();
        assertThat(rs.wasNull()).isFalse();
        assertThat(rs.getObject(2)).isNull();
        assertThat(rs.wasNull()).isTrue();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getObject(2)).isNull();
        assertThat(rs.wasNull()).isTrue();
        assertThat(rs.getObject(1)).isNull();
        assertThat(rs.wasNull()).isTrue();
        assertThat(rs.next()).isFalse();
        rs.close();
    }

    @Test
    public void tempTable() throws SQLException {
        assertThat(stat.executeUpdate("create temp table myTemp (a);")).isEqualTo(0);
        assertThat(stat.executeUpdate("insert into myTemp values (2);")).isEqualTo(1);
    }

    @Test
    public void insert1000() throws SQLException {
        assertThat(stat.executeUpdate("create table in1000 (a);")).isEqualTo(0);
        conn.setAutoCommit(false);
        for (int i = 0; i < 1000; i++) {
            assertThat(stat.executeUpdate("insert into in1000 values (" + i + ");")).isEqualTo(1);
        }
        conn.commit();

        ResultSet rs = stat.executeQuery("select count(a) from in1000;");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(1000);
        rs.close();

        assertThat(stat.executeUpdate("drop table in1000;")).isEqualTo(0);
    }

    @Test
    public void batch() throws SQLException {
        stat.addBatch("create table batch (c1);");
        stat.addBatch("insert into batch values (1);");
        stat.addBatch("insert into batch values (1);");
        stat.addBatch("insert into batch values (2);");
        stat.addBatch("insert into batch values (3);");
        stat.addBatch("insert into batch values (4);");
        assertThat(stat.executeBatch()).containsExactly(0, 1, 1, 1, 1, 1);
        assertThat(stat.executeBatch()).isEmpty();
        stat.clearBatch();
        stat.addBatch("insert into batch values (9);");
        assertThat(stat.executeBatch()).containsExactly(1);
        assertThat(stat.executeBatch()).isEmpty();
        stat.clearBatch();
        stat.addBatch("insert into batch values (7);");
        stat.addBatch("insert into batch values (7);");
        assertThat(stat.executeBatch()).containsExactly(1, 1);
        stat.clearBatch();

        ResultSet rs = stat.executeQuery("select count(*) from batch;");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(8);
        rs.close();
    }

    @Test
    public void closeOnFalseNext() throws SQLException {
        stat.executeUpdate("create table t1 (c1);");
        conn.createStatement().executeQuery("select * from t1;").next();
        stat.executeUpdate("drop table t1;");
    }

    @Test
    public void getGeneratedKeys() throws SQLException {
        ResultSet rs;
        stat.executeUpdate("create table t1 (c1 integer primary key, v);");

        // test standard insert operation
        stat.executeUpdate("insert into t1 (v) values ('red');");
        rs = stat.getGeneratedKeys();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(1);
        rs.close();

        stat.executeUpdate("insert into t1 (v) values ('blue');");
        rs = stat.getGeneratedKeys();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(2);
        rs.close();

        // test INSERT ith special replace keyword. This will trigger a primary key conflict on the
        // first
        // inserted row ('red') and replace the record with a value of 'yellow' with the same
        // primary
        // key. The value returned from getGeneratedKeys should be primary key of the replaced
        // record
        stat.executeUpdate("replace into t1 (c1, v) values (1, 'yellow');");
        rs = stat.getGeneratedKeys();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(1);
        rs.close();

        // test INSERT with common table expression
        stat.executeUpdate(
                "with colors as (select 'green' as color)\n"
                        + "insert into t1 (v) select color from colors;");
        rs = stat.getGeneratedKeys();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(3);
        rs.close();

        stat.close();

        // generated keys are now attached to the statement. calling getGeneratedKeys
        // on a statement that has not generated any should return an empty result set
        Statement stat2 = conn.createStatement();
        stat.executeUpdate(
                "with colors as (select 'insert' as color) update t1 set v = (select color from colors);");
        rs = stat2.getGeneratedKeys();
        assertThat(rs).isNotNull();
        assertThat(rs.next()).isFalse();
        stat2.close();
    }

    @Test
    public void getGeneratedKeysIsStatementSpecific() throws SQLException {
        /* this test ensures that the results of getGeneratedKeys are tied to
          a specific statement. To verify this, we create two separate Statement
          objects and then execute inserts on both. We then make getGeneratedKeys()
          calls and verify that the two separate expected values are returned.

          Note that the old implementation of getGeneratedKeys was called lazily, so
          the result of both getGeneratedKeys calls would be the same value, the row ID
          of the last insert on the connection. As a result it was unsafe to use
          with multiple statements or in a multithreaded application.
        */
        stat.executeUpdate("create table t1 (c1 integer primary key, v);");

        ResultSet rs1;
        Statement stat1 = conn.createStatement();
        ResultSet rs2;
        Statement stat2 = conn.createStatement();

        stat1.executeUpdate("insert into t1 (v) values ('red');");
        stat2.executeUpdate("insert into t1 (v) values ('blue');");

        rs2 = stat2.getGeneratedKeys();
        rs1 = stat1.getGeneratedKeys();

        assertThat(rs1.next()).isTrue();
        assertThat(rs1.getInt(1)).isEqualTo(1);
        rs1.close();

        assertThat(rs2.next()).isTrue();
        assertThat(rs2.getInt(1)).isEqualTo(2);
        rs2.close();

        stat1.close();
        stat2.close();
    }

    @Test
    public void isBeforeFirst() throws SQLException {
        ResultSet rs = stat.executeQuery("select 1 union all select 2;");
        assertThat(rs.isBeforeFirst()).isTrue();
        assertThat(rs.next()).isTrue();
        assertThat(rs.isFirst()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(1);
        assertThat(rs.next()).isTrue();
        assertThat(rs.isBeforeFirst()).isFalse();
        assertThat(rs.isFirst()).isFalse();
        assertThat(rs.getInt(1)).isEqualTo(2);
        assertThat(rs.next()).isFalse();
        assertThat(rs.isBeforeFirst()).isFalse();
        rs.close();
    }

    @Test
    public void columnNaming() throws SQLException {
        stat.executeUpdate("create table t1 (c1 integer);");
        stat.executeUpdate("create table t2 (c1 integer);");
        stat.executeUpdate("insert into t1 values (1);");
        stat.executeUpdate("insert into t2 values (1);");
        ResultSet rs = stat.executeQuery("select a.c1 AS c1 from t1 a, t2 where a.c1=t2.c1;");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt("c1")).isEqualTo(1);
        rs.close();
    }

    @Test
    public void nullDate() throws SQLException {
        ResultSet rs = stat.executeQuery("select null;");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDate(1)).isNull();
        assertThat(rs.getTime(1)).isNull();
        assertThat(rs.getTimestamp(1)).isNull();

        assertThat(rs.getDate(1, Calendar.getInstance())).isNull();
        assertThat(rs.getTime(1, Calendar.getInstance())).isNull();
        assertThat(rs.getTimestamp(1, Calendar.getInstance())).isNull();
        rs.close();
    }

    @Test
    public void emptyDate() throws SQLException {
        ResultSet rs = stat.executeQuery("select '';");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDate(1)).isNull();
        assertThat(rs.getTime(1)).isNull();
        assertThat(rs.getTimestamp(1)).isNull();

        assertThat(rs.getDate(1, Calendar.getInstance())).isNull();
        assertThat(rs.getTime(1, Calendar.getInstance())).isNull();
        assertThat(rs.getTimestamp(1, Calendar.getInstance())).isNull();
        rs.close();
    }

    @Disabled
    @Test
    public void ambiguousColumnNaming() throws SQLException {
        stat.executeUpdate("create table t1 (c1 int);");
        stat.executeUpdate("create table t2 (c1 int, c2 int);");
        stat.executeUpdate("insert into t1 values (1);");
        stat.executeUpdate("insert into t2 values (2, 1);");
        ResultSet rs = stat.executeQuery("select a.c1, b.c1 from t1 a, t2 b where a.c1=b.c2;");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt("c1")).isEqualTo(1);
        rs.close();
    }

    @Test
    public void failToDropWhenRSOpen() throws SQLException {
        stat.executeUpdate("create table t1 (c1);");
        stat.executeUpdate("insert into t1 values (4);");
        stat.executeUpdate("insert into t1 values (4);");
        conn.createStatement().executeQuery("select * from t1;").next();
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> stat.executeUpdate("drop table t1;"));
    }

    @Test
    public void executeNoRS() {
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> stat.execute("insert into test values (8);"));
    }

    @Test
    public void executeClearRS() throws SQLException {
        assertThat(stat.execute("select null;")).isTrue();
        assertThat(stat.getResultSet()).isNotNull();
        assertThatExceptionOfType(SQLException.class)
                .as("requesting the same result set twice should throw an exception")
                .isThrownBy(() -> stat.getResultSet());
        assertThat(stat.getMoreResults()).isFalse();
        assertThat(stat.isClosed()).isFalse();
        assertThat(stat.getResultSet()).isNull();
        assertThat(stat.getUpdateCount()).isEqualTo(-1);
    }

    @Test
    public void getMoreResultsArguments() throws SQLException {
        assertThat(stat.execute("select null;")).isTrue();
        assertThat(stat.getResultSet()).isNotNull();
        assertThatExceptionOfType(SQLException.class)
                .as("getMoreResults only accepts valid arguments")
                .isThrownBy(() -> stat.getMoreResults(15));
        assertThatExceptionOfType(SQLFeatureNotSupportedException.class)
                .as("getMoreResults with CLOSE_ALL_RESULTS is not supported")
                .isThrownBy(() -> stat.getMoreResults(Statement.CLOSE_ALL_RESULTS));
        assertThatExceptionOfType(SQLFeatureNotSupportedException.class)
                .as("getMoreResults with KEEP_CURRENT_RESULT is not supported")
                .isThrownBy(() -> stat.getMoreResults(Statement.KEEP_CURRENT_RESULT));
    }

    @Test
    public void batchReturnsResults() throws SQLException {
        stat.addBatch("select null;");
        assertThatExceptionOfType(BatchUpdateException.class).isThrownBy(() -> stat.executeBatch());
    }

    @Test
    public void noSuchTable() {
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> stat.executeQuery("select * from doesnotexist;"));
    }

    @Test
    public void noSuchCol() {
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> stat.executeQuery("select notacol from (select 1);"));
    }

    @Test
    public void noSuchColName() throws SQLException {
        ResultSet rs = stat.executeQuery("select 1;");
        assertThat(rs.next()).isTrue();
        assertThatExceptionOfType(SQLException.class).isThrownBy(() -> rs.getInt("noSuchColName"));
    }

    @Test
    public void multipleStatements() throws SQLException {
        // ; insert into person values(1,'leo')
        stat.executeUpdate(
                "create table person (id integer, name string); "
                        + "insert into person values(1, 'leo'); insert into person values(2, 'yui');");
        ResultSet rs = stat.executeQuery("select * from person");
        assertThat(rs.next()).isTrue();
        assertThat(rs.next()).isTrue();
    }

    @Test
    public void blobTest() throws SQLException {
        stat.executeUpdate("CREATE TABLE Foo (KeyId INTEGER, Stuff BLOB)");
    }

    @Test
    public void bytesTest() throws SQLException {
        stat.executeUpdate("CREATE TABLE blobs (Blob BLOB)");
        PreparedStatement prep = conn.prepareStatement("insert into blobs values(?)");

        String str = "This is a test";
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);

        prep.setBytes(1, strBytes);
        prep.executeUpdate();

        ResultSet rs = stat.executeQuery("select * from blobs");
        assertThat(rs.next()).isTrue();

        byte[] resultBytes = rs.getBytes(1);
        assertThat(resultBytes).isEqualTo(strBytes);

        String resultStr = rs.getString(1);
        assertThat(resultStr).isEqualTo(str);

        byte[] resultBytesAfterConversionToString = rs.getBytes(1);
        assertThat(resultBytesAfterConversionToString).isEqualTo(strBytes);
    }

    @Test
    public void dateTimeTest() throws SQLException {
        Date day = new Date(new java.util.Date().getTime());

        stat.executeUpdate("create table day (time datetime)");
        PreparedStatement prep = conn.prepareStatement("insert into day values(?)");
        prep.setDate(1, day);
        prep.executeUpdate();
        ResultSet rs = stat.executeQuery("select * from day");
        assertThat(rs.next()).isTrue();
        Date d = rs.getDate(1);
        assertThat(d.getTime()).isEqualTo(day.getTime());
    }

    @Test
    public void defaultDateTimeTest() throws SQLException {
        stat.executeUpdate(
                "create table daywithdefaultdatetime (id integer, datetime datatime default current_timestamp)");
        PreparedStatement prep =
                conn.prepareStatement("insert into daywithdefaultdatetime (id) values (?)");
        prep.setInt(1, 1);
        prep.executeUpdate();
        ResultSet rs = stat.executeQuery("select * from daywithdefaultdatetime");
        assertThat(rs.next()).isTrue();
        Date d = rs.getDate(2);
        assertThat(d).isNotNull();
    }

    @Test
    public void maxRows() throws SQLException {
        stat.setMaxRows(1);
        ResultSet rs = stat.executeQuery("select 1 union select 2 union select 3");

        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(1);
        assertThat(rs.next()).isFalse();

        rs.close();
        stat.setMaxRows(2);
        rs = stat.executeQuery("select 1 union select 2 union select 3");

        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(1);
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(2);
        assertThat(rs.next()).isFalse();

        rs.close();
    }

    @Test
    public void setEscapeProcessingToFalse() {
        assertThatNoException().isThrownBy(() -> stat.setEscapeProcessing(false));
    }

    @Test
    public void setEscapeProcessingToTrue() {
        assertThatNoException().isThrownBy(() -> stat.setEscapeProcessing(true));
    }

    @Test
    public void unwrapTest() throws SQLException {
        assertThat(conn.isWrapperFor(Connection.class)).isTrue();
        assertThat(conn.isWrapperFor(Statement.class)).isFalse();
        assertThat(conn.unwrap(Connection.class)).isEqualTo(conn);
        assertThat(conn.unwrap(SQLiteConnection.class)).isEqualTo(conn);

        assertThat(stat.isWrapperFor(Statement.class)).isTrue();
        assertThat(stat.unwrap(Statement.class)).isEqualTo(stat);
        assertThat(stat.unwrap(JDBC3Statement.class)).isEqualTo(stat);

        ResultSet rs = stat.executeQuery("select 1");

        assertThat(rs.isWrapperFor(ResultSet.class)).isTrue();
        assertThat(rs.unwrap(ResultSet.class)).isEqualTo(rs);

        rs.close();
    }

    @Test
    public void closeOnCompletionTest() throws Exception {
        if (!(stat instanceof JDBC4Statement)) {
            return;
        }

        // Run the following code only for JDK7 or higher
        Method mIsCloseOnCompletion = JDBC4Statement.class.getDeclaredMethod("isCloseOnCompletion");
        Method mCloseOnCompletion = JDBC4Statement.class.getDeclaredMethod("closeOnCompletion");
        assertThat((Boolean) mIsCloseOnCompletion.invoke(stat)).isFalse();

        mCloseOnCompletion.invoke(stat);
        assertThat((Boolean) mIsCloseOnCompletion.invoke(stat)).isTrue();

        ResultSet rs = stat.executeQuery("select 1");
        rs.close();

        assertThat(stat.isClosed()).isTrue();
    }

    @Test
    public void setFetchDirection() throws SQLException {
        stat.setFetchDirection(ResultSet.FETCH_FORWARD);
        stat.setFetchDirection(ResultSet.FETCH_REVERSE);
        stat.setFetchDirection(ResultSet.FETCH_UNKNOWN);
    }

    @Test
    public void setFetchDirectionBadArgument() {
        assertThatExceptionOfType(SQLException.class).isThrownBy(() -> stat.setFetchDirection(999));
    }

    @Test
    public void getFetchDirection() throws SQLException {
        assertThat(stat.getFetchDirection()).isEqualTo(ResultSet.FETCH_FORWARD);
    }

    @Test
    public void unixepoch() throws SQLException {
        ResultSet rs = stat.executeQuery("select unixepoch()");
        long javaEpoch = Instant.now().getEpochSecond();

        assertThat(rs.getLong(1)).isCloseTo(javaEpoch, offset(1L));
    }
}
