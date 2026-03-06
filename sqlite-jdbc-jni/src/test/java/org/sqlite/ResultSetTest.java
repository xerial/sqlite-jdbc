package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResultSetTest {

    private Connection conn;
    private Statement stat;

    @BeforeEach
    public void connect() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
        stat.executeUpdate(
                "create table test (id int primary key, DESCRIPTION varchar(40), fOo varchar(3));");
        stat.executeUpdate("insert into test values (1, 'description', 'bar')");
    }

    @AfterEach
    public void close() throws SQLException {
        stat.close();
        conn.close();
    }

    @Test
    void testTableColumnLowerNowFindLowerCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("id")).isEqualTo(1);
    }

    @Test
    void testTableColumnLowerNowFindUpperCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("ID")).isEqualTo(1);
    }

    @Test
    void testTableColumnLowerNowFindMixedCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("Id")).isEqualTo(1);
    }

    @Test
    void testTableColumnUpperNowFindLowerCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("description")).isEqualTo(2);
    }

    @Test
    void testTableColumnUpperNowFindUpperCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("DESCRIPTION")).isEqualTo(2);
    }

    @Test
    void testTableColumnUpperNowFindMixedCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("Description")).isEqualTo(2);
    }

    @Test
    void testTableColumnMixedNowFindLowerCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("foo")).isEqualTo(3);
    }

    @Test
    void testTableColumnMixedNowFindUpperCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("FOO")).isEqualTo(3);
    }

    @Test
    void testTableColumnMixedNowFindMixedCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("fOo")).isEqualTo(3);
    }

    @Test
    void testSelectWithTableNameAliasNowFindWithoutTableNameAlias() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select t.id from test as t");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("id")).isEqualTo(1);
    }

    /**
     * Can't produce a case where column name contains table name
     * https://www.sqlite.org/c3ref/column_name.html : "If there is no AS clause then the name of
     * the column is unspecified"
     */
    @Test
    void testSelectWithTableNameAliasNowNotFindWithTableNameAlias() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select t.id from test as t");
        assertThat(resultSet.next()).isTrue();
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> resultSet.findColumn("t.id"));
    }

    @Test
    void testSelectWithTableNameNowFindWithoutTableName() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select test.id from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("id")).isEqualTo(1);
    }

    @Test
    void testSelectWithTableNameNowNotFindWithTableName() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select test.id from test");
        assertThat(resultSet.next()).isTrue();
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> resultSet.findColumn("test.id"));
    }

    @Test
    void testCloseStatement() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select test.id from test");

        stat.close();

        assertThat(stat.isClosed()).isTrue();
        assertThat(resultSet.isClosed()).isTrue();

        resultSet.close();

        assertThat(resultSet.isClosed()).isTrue();
    }

    @Test
    void testReturnsNonAsciiCodepoints() throws SQLException {
        String nonAsciiString = "국정의 중요한 사항에 관한";
        PreparedStatement pstat = conn.prepareStatement("select ?");
        pstat.setString(1, nonAsciiString);

        ResultSet resultSet = pstat.executeQuery();

        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.getString(1)).isEqualTo(nonAsciiString);
        assertThat(resultSet.next()).isFalse();
    }

    @Test
    void testFindColumnOnEmptyResultSet() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test where id = 0");
        assertThat(resultSet.next()).isFalse();
        assertThat(resultSet.findColumn("id")).isEqualTo(1);
    }

    @Test
    void testNumericTypes() throws SQLException {
        stat.executeUpdate("create table numeric(c1, c2, c3)");
        stat.executeUpdate("insert into numeric values (1, 1.1, null)");

        ResultSet rs = stat.executeQuery("select * from numeric");

        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(1);
        assertThat(rs.getInt(2)).isEqualTo(1);
        assertThat(rs.getInt(3)).isEqualTo(0);
        assertThat(rs.getLong(1)).isEqualTo(1L);
        assertThat(rs.getLong(2)).isEqualTo(1L);
        assertThat(rs.getLong(3)).isEqualTo(0L);
        assertThat(rs.getDouble(1)).isEqualTo(1.0D);
        assertThat(rs.getDouble(2)).isEqualTo(1.1D);
        assertThat(rs.getDouble(3)).isEqualTo(0D);
        assertThat(rs.getFloat(1)).isEqualTo(1.0F);
        assertThat(rs.getFloat(2)).isEqualTo(1.1F);
        assertThat(rs.getFloat(3)).isEqualTo(0F);
        assertThat(rs.getString(1)).isEqualTo("1");
        assertThat(rs.getString(2)).isEqualTo("1.1");
        assertThat(rs.getString(3)).isNull();
    }

    @Test
    void testGetBigDecimal() throws SQLException {
        stat.executeUpdate(
                "create table bigdecimal(c1, c2 integer, c3 real, c4 double, c5 decimal, c6 numeric, c7 float)");
        stat.executeUpdate("insert into bigdecimal values (1, 2, 3, 4, 5, 6, 7)");
        stat.executeUpdate("insert into bigdecimal values ('1', '2', '3', '4', '5', '6', '7')");
        stat.executeUpdate("insert into bigdecimal values (1.1, 2.1, 3.1, 4.1, 5.1, 6.1, 7.1)");
        stat.executeUpdate(
                "insert into bigdecimal values ('1.1', '2.1', '3.1', '4.1', '5.1', '6.1', '7.1')");
        stat.executeUpdate(
                "insert into bigdecimal values (null, null, null, null, null, null, null)");
        stat.executeUpdate(
                "insert into bigdecimal values ('null', '', 'abc', 'abc', 'abc', 'abc', 'abc')");

        ResultSet rs = stat.executeQuery("select * from bigdecimal");

        assertThat(rs.next()).isTrue();
        assertThat(rs.getBigDecimal(1)).isEqualTo(new BigDecimal("1"));
        assertThat(rs.getBigDecimal(2)).isEqualTo(new BigDecimal("2"));
        assertThat(rs.getBigDecimal(3)).isEqualTo(new BigDecimal("3.0"));
        assertThat(rs.getBigDecimal(4)).isEqualTo(new BigDecimal("4.0"));
        assertThat(rs.getBigDecimal(5)).isEqualTo(new BigDecimal("5"));
        assertThat(rs.getBigDecimal(6)).isEqualTo(new BigDecimal("6"));
        assertThat(rs.getBigDecimal(7)).isEqualTo(new BigDecimal("7.0"));

        assertThat(rs.next()).isTrue();
        assertThat(rs.getBigDecimal(1)).isEqualTo(new BigDecimal("1"));
        assertThat(rs.getBigDecimal(2)).isEqualTo(new BigDecimal("2"));
        assertThat(rs.getBigDecimal(3)).isEqualTo(new BigDecimal("3.0"));
        assertThat(rs.getBigDecimal(4)).isEqualTo(new BigDecimal("4.0"));
        assertThat(rs.getBigDecimal(5)).isEqualTo(new BigDecimal("5"));
        assertThat(rs.getBigDecimal(6)).isEqualTo(new BigDecimal("6"));
        assertThat(rs.getBigDecimal(7)).isEqualTo(new BigDecimal("7.0"));

        assertThat(rs.next()).isTrue();
        assertThat(rs.getBigDecimal(1)).isEqualTo(new BigDecimal("1.1"));
        assertThat(rs.getBigDecimal(2)).isEqualTo(new BigDecimal("2.1"));
        assertThat(rs.getBigDecimal(3)).isEqualTo(new BigDecimal("3.1"));
        assertThat(rs.getBigDecimal(4)).isEqualTo(new BigDecimal("4.1"));
        assertThat(rs.getBigDecimal(5)).isEqualTo(new BigDecimal("5.1"));
        assertThat(rs.getBigDecimal(6)).isEqualTo(new BigDecimal("6.1"));
        assertThat(rs.getBigDecimal(7)).isEqualTo(new BigDecimal("7.1"));

        assertThat(rs.next()).isTrue();
        assertThat(rs.getBigDecimal(1)).isEqualTo(new BigDecimal("1.1"));
        assertThat(rs.getBigDecimal(2)).isEqualTo(new BigDecimal("2.1"));
        assertThat(rs.getBigDecimal(3)).isEqualTo(new BigDecimal("3.1"));
        assertThat(rs.getBigDecimal(4)).isEqualTo(new BigDecimal("4.1"));
        assertThat(rs.getBigDecimal(5)).isEqualTo(new BigDecimal("5.1"));
        assertThat(rs.getBigDecimal(6)).isEqualTo(new BigDecimal("6.1"));
        assertThat(rs.getBigDecimal(7)).isEqualTo(new BigDecimal("7.1"));

        assertThat(rs.next()).isTrue();
        assertThat(rs.getBigDecimal(1)).isNull();
        assertThat(rs.getBigDecimal(2)).isNull();
        assertThat(rs.getBigDecimal(3)).isNull();
        assertThat(rs.getBigDecimal(4)).isNull();
        assertThat(rs.getBigDecimal(5)).isNull();
        assertThat(rs.getBigDecimal(6)).isNull();
        assertThat(rs.getBigDecimal(7)).isNull();

        assertThat(rs.next()).isTrue();
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> rs.getBigDecimal(1))
                .withMessageContaining("Bad value for type BigDecimal");
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> rs.getBigDecimal(2))
                .withMessageContaining("Bad value for type BigDecimal");
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> rs.getBigDecimal(3))
                .withMessageContaining("Bad value for type BigDecimal");
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> rs.getBigDecimal(4))
                .withMessageContaining("Bad value for type BigDecimal");
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> rs.getBigDecimal(5))
                .withMessageContaining("Bad value for type BigDecimal");
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> rs.getBigDecimal(6))
                .withMessageContaining("Bad value for type BigDecimal");
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> rs.getBigDecimal(7))
                .withMessageContaining("Bad value for type BigDecimal");

        assertThat(rs.next()).isFalse();
    }

    @Test
    void getObjectWithRequestedType() throws SQLException {
        stat.executeUpdate("create table getobject(c1)");
        stat.executeUpdate("insert into getobject values (1)");
        stat.executeUpdate("insert into getobject values ('abc')");

        ResultSet rs = stat.executeQuery("select * from getobject");

        assertThat(rs.next()).isTrue();
        assertThat(rs.getObject(1, String.class)).isEqualTo("1");
        assertThat(rs.getObject(1, Boolean.class)).isTrue();
        assertThat(rs.getObject(1, BigDecimal.class)).isEqualTo(rs.getBigDecimal(1));
        assertThat(rs.getObject(1, byte[].class)).isEqualTo(rs.getBytes(1));
        assertThat(rs.getObject(1, Double.class)).isEqualTo(rs.getDouble(1));
        assertThat(rs.getObject(1, Long.class)).isEqualTo(rs.getLong(1));
        assertThat(rs.getObject(1, Float.class)).isEqualTo(rs.getFloat(1));
        assertThat(rs.getObject(1, Integer.class)).isEqualTo(rs.getInt(1));
        assertThat(rs.getObject(1, Date.class)).isEqualTo(rs.getDate(1));
        assertThat(rs.getObject(1, Time.class)).isEqualTo(rs.getTime(1));
        assertThat(rs.getObject(1, Timestamp.class)).isEqualTo(rs.getTimestamp(1));

        assertThat(rs.next()).isTrue();
        assertThat(rs.getObject(1, String.class)).isEqualTo("abc");
        assertThat(rs.getObject(1, Boolean.class)).isFalse();
        assertThat(rs.getObject(1, byte[].class)).isEqualTo(rs.getBytes(1));
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> rs.getObject(1, BigDecimal.class))
                .withMessageContaining("Bad value for type BigDecimal");
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> rs.getObject(1, Double.class))
                .withMessageContaining("Bad value for type Double");
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> rs.getObject(1, Long.class))
                .withMessageContaining("Bad value for type Long");
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> rs.getObject(1, Float.class))
                .withMessageContaining("Bad value for type Float");
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> rs.getObject(1, Integer.class))
                .withMessageContaining("Bad value for type Integer");

        assertThat(rs.next()).isFalse();
    }

    @Test
    void testJdk8AddedDateTimeObjects() throws SQLException {
        stat.executeUpdate("create table datetime_test(c1)");
        stat.executeUpdate("insert into datetime_test values ('2021-11-09 11:20:58')");
        stat.executeUpdate("insert into datetime_test values ('2021-11-09')");
        stat.executeUpdate("insert into datetime_test values ('11:20:58')");
        stat.executeUpdate("insert into datetime_test values (NULL)");

        ResultSet rs = stat.executeQuery("select * from datetime_test");

        rs.next();
        assertThat(rs.getObject(1, LocalDate.class)).isEqualTo(LocalDate.of(2021, 11, 9));
        assertThat(rs.getObject(1, LocalTime.class)).isEqualTo(LocalTime.of(11, 20, 58));
        assertThat(rs.getObject(1, LocalDateTime.class))
                .isEqualTo(LocalDateTime.of(2021, 11, 9, 11, 20, 58));

        rs.next();
        assertThat(rs.getObject(1, LocalDate.class)).isEqualTo(LocalDate.of(2021, 11, 9));

        rs.next();
        assertThat(rs.getObject(1, LocalTime.class)).isEqualTo(LocalTime.of(11, 20, 58));

        rs.next();
        assertThat(rs.getObject(1, LocalDate.class)).isNull();
        assertThat(rs.getObject(1, LocalTime.class)).isNull();
        assertThat(rs.getObject(1, LocalDateTime.class)).isNull();
    }

    @Test
    void gh808_getResultSetMetadataAfterReadingLastRow() throws SQLException {
        for (int i = 0; i < 2; i++) {
            ResultSet rs = stat.executeQuery("select 1");
            assertThat(rs).isNotNull();

            assertThat(rs.next()).isTrue();
            assertThat(rs.isAfterLast()).isFalse();

            assertThat(rs.next()).isFalse();
            assertThat(rs.isClosed()).isFalse();
            assertThat(rs.isAfterLast()).isTrue();

            ResultSetMetaData meta = rs.getMetaData();
            assertThat(meta).isNotNull();
            assertThat(meta.getColumnCount()).isEqualTo(1);
        }
    }
}
