// --------------------------------------
// sqlite-jdbc Project
//
// QueryTest.java
// Since: Apr 8, 2009
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.data.Offset.offset;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.sqlite.date.FastDateFormat;

public class QueryTest {
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    @Test
    public void nullQuery() throws Exception {
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                assertThatExceptionOfType(NullPointerException.class)
                        .isThrownBy(() -> stmt.execute(null));
            }
        }
    }

    @Test
    public void createTable() throws Exception {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute(
                "CREATE TABLE IF NOT EXISTS sample "
                        + "(id INTEGER PRIMARY KEY, descr VARCHAR(40))");
        stmt.close();

        stmt = conn.createStatement();
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM sample");
            rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        conn.close();
    }

    @Test
    public void setFloatTest() throws Exception {
        float f = 3.141597f;
        Connection conn = getConnection();

        conn.createStatement().execute("create table sample (data NOAFFINITY)");
        PreparedStatement prep = conn.prepareStatement("insert into sample values(?)");
        prep.setFloat(1, f);
        prep.executeUpdate();

        PreparedStatement stmt = conn.prepareStatement("select * from sample where data > ?");
        stmt.setObject(1, 3.0f);
        ResultSet rs = stmt.executeQuery();
        assertThat(rs.next()).isTrue();
        float f2 = rs.getFloat(1);
        assertThat(f2).isCloseTo(f, offset(0.0000001F));
    }

    @Test
    public void dateTimeTest() throws Exception {
        Connection conn = getConnection();

        conn.createStatement().execute("create table sample (start_time datetime)");

        Date now = new Date();
        String date =
                FastDateFormat.getInstance(SQLiteConfig.DEFAULT_DATE_STRING_FORMAT).format(now);

        conn.createStatement().execute("insert into sample values(" + now.getTime() + ")");
        conn.createStatement().execute("insert into sample values('" + date + "')");

        ResultSet rs = conn.createStatement().executeQuery("select * from sample");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDate(1)).isEqualTo(now);
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDate(1)).isEqualTo(now);

        PreparedStatement stmt = conn.prepareStatement("insert into sample values(?)");
        stmt.setDate(1, new java.sql.Date(now.getTime()));
    }

    @Test
    public void jdk8LocalDateTimeTest() throws Exception {
        Connection conn = getConnection();

        conn.createStatement().execute("create table sample (d1 date, d2 time, d3 datetime)");

        LocalDateTime dateTime = LocalDateTime.of(2022, 1, 1, 12, 25, 15);
        try (PreparedStatement stmt = conn.prepareStatement("insert into sample values(?, ?, ?)")) {
            stmt.setObject(1, dateTime.toLocalDate());
            stmt.setObject(2, dateTime.toLocalTime());
            stmt.setObject(3, dateTime);
            stmt.executeUpdate();
        }

        try (ResultSet rs = conn.createStatement().executeQuery("select * from sample")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getObject(1, LocalDate.class)).isEqualTo(dateTime.toLocalDate());
            assertThat(rs.getObject(2, LocalTime.class)).isEqualTo(dateTime.toLocalTime());
            assertThat(rs.getObject(3, LocalDateTime.class)).isEqualTo(dateTime);
        }
    }

    @Test
    public void dateTimeWithTimeZoneTest() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(SQLiteConfig.Pragma.DATE_CLASS.pragmaName, "text");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:", properties);

        try (Statement statement = conn.createStatement()) {
            statement.execute("create table sample (date_time datetime)");
        }

        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        TimeZone customTimeZone = TimeZone.getTimeZone("+3");
        Calendar utcCalendar = Calendar.getInstance(utcTimeZone);
        Calendar customCalendar = Calendar.getInstance(customTimeZone);

        java.sql.Date now = new java.sql.Date(new Date().getTime());
        FastDateFormat customFormat =
                FastDateFormat.getInstance(SQLiteConfig.DEFAULT_DATE_STRING_FORMAT, customTimeZone);
        FastDateFormat utcFormat =
                FastDateFormat.getInstance(SQLiteConfig.DEFAULT_DATE_STRING_FORMAT, utcTimeZone);
        java.sql.Date nowLikeCustomZoneIsUtc =
                new java.sql.Date(utcFormat.parse(customFormat.format(now)).getTime());

        try (PreparedStatement preparedStatement =
                conn.prepareStatement("insert into sample (date_time) values(?)")) {
            preparedStatement.setDate(1, now, customCalendar);
            preparedStatement.executeUpdate();
            preparedStatement.setDate(1, nowLikeCustomZoneIsUtc, utcCalendar);
            preparedStatement.executeUpdate();
        }

        try (ResultSet resultSet = conn.createStatement().executeQuery("select * from sample")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getDate(1, customCalendar)).isEqualTo(now);
            assertThat(resultSet.getDate(1, utcCalendar)).isEqualTo(nowLikeCustomZoneIsUtc);

            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getDate(1, customCalendar)).isEqualTo(now);
            assertThat(resultSet.getDate(1, utcCalendar)).isEqualTo(nowLikeCustomZoneIsUtc);
        }
    }

    @Test
    public void notEmptyBlob() throws Exception {
        Connection conn = getConnection();

        conn.createStatement().execute("create table sample (b blob not null)");

        conn.createStatement().execute("insert into sample values(zeroblob(5))");

        ResultSet rs = conn.createStatement().executeQuery("select * from sample");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getBytes(1).length).isEqualTo(5);
        assertThat(rs.wasNull()).isFalse();
    }

    @Test
    public void emptyBlob() throws Exception {
        Connection conn = getConnection();

        conn.createStatement().execute("create table sample (b blob null)");

        conn.createStatement().execute("insert into sample values(zeroblob(0))");

        ResultSet rs = conn.createStatement().executeQuery("select * from sample");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getBytes(1).length).isEqualTo(0);
        assertThat(rs.wasNull()).isFalse();
    }

    @Test
    public void nullBlob() throws Exception {
        Connection conn = getConnection();

        conn.createStatement().execute("create table sample (b blob null)");

        conn.createStatement().execute("insert into sample values(null)");

        ResultSet rs = conn.createStatement().executeQuery("select * from sample");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getBytes(1)).isNull();
        assertThat(rs.wasNull()).isTrue();
    }

    @Test
    public void viewTest() throws Exception {
        Connection conn = getConnection();
        Statement st1 = conn.createStatement();
        // drop table if it already exists

        String tableName = "sample";
        st1.execute("DROP TABLE IF EXISTS " + tableName);
        st1.close();
        Statement st2 = conn.createStatement();
        st2.execute("DROP VIEW IF EXISTS " + tableName);
        st2.close();
    }

    @Test
    public void timeoutTest() throws Exception {
        Connection conn = getConnection();
        Statement st1 = conn.createStatement();

        st1.setQueryTimeout(1);

        st1.close();
    }

    @Test
    public void concatTest() throws SQLException {
        try (Connection conn = getConnection()) {
            // create a database connection
            Statement statement = conn.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            statement.executeUpdate("drop table if exists person");
            statement.executeUpdate(
                    "create table person (id integer, name string, shortname string)");
            statement.executeUpdate("insert into person values(1, 'leo','L')");
            statement.executeUpdate("insert into person values(2, 'yui','Y')");
            statement.executeUpdate("insert into person values(3, 'abc', null)");

            statement.executeUpdate("drop table if exists message");
            statement.executeUpdate("create table message (id integer, subject string)");
            statement.executeUpdate("insert into message values(1, 'Hello')");
            statement.executeUpdate("insert into message values(2, 'World')");

            statement.executeUpdate("drop table if exists mxp");
            statement.executeUpdate("create table mxp (pid integer, mid integer, type string)");
            statement.executeUpdate("insert into mxp values(1,1, 'F')");
            statement.executeUpdate("insert into mxp values(2,1,'T')");
            statement.executeUpdate("insert into mxp values(1,2, 'F')");
            statement.executeUpdate("insert into mxp values(2,2,'T')");
            statement.executeUpdate("insert into mxp values(3,2,'T')");

            ResultSet rs =
                    statement.executeQuery(
                            "select group_concat(ifnull(shortname, name)) from mxp, person where mxp.mid=2 and mxp.pid=person.id and mxp.type='T'");
            while (rs.next()) {
                // read the result set
                assertThat(rs.getString(1)).isEqualTo("Y,abc");
            }
            rs =
                    statement.executeQuery(
                            "select group_concat(ifnull(shortname, name)) from mxp, person where mxp.mid=1 and mxp.pid=person.id and mxp.type='T'");
            while (rs.next()) {
                // read the result set
                assertThat(rs.getString(1)).isEqualTo("Y");
            }

            PreparedStatement ps =
                    conn.prepareStatement(
                            "select group_concat(ifnull(shortname, name)) from mxp, person where mxp.mid=? and mxp.pid=person.id and mxp.type='T'");
            ps.clearParameters();
            ps.setInt(1, 2);
            rs = ps.executeQuery();
            while (rs.next()) {
                // read the result set
                assertThat(rs.getString(1)).isEqualTo("Y,abc");
            }
            ps.clearParameters();
            ps.setInt(1, 2);
            rs = ps.executeQuery();
            while (rs.next()) {
                // read the result set
                assertThat(rs.getString(1)).isEqualTo("Y,abc");
            }
        }
    }

    @Test
    public void clobTest() throws SQLException {
        String content = "test_clob";
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("select cast(? as clob)")) {
                stmt.setString(1, content);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    Clob clob = rs.getClob(1);
                    int length = (int) clob.length();
                    assertThatExceptionOfType(SQLException.class)
                            .isThrownBy(() -> clob.getSubString(0, length));
                    assertThatExceptionOfType(SQLException.class)
                            .isThrownBy(() -> clob.getSubString(1, -1));
                    assertThat(clob.getSubString(1, 0)).isEqualTo("");
                    assertThat(clob.getSubString(1, length)).isEqualTo(content);
                    assertThat(clob.getSubString(3, content.length() - 3))
                            .isEqualTo(content.substring(2, content.length() - 1));
                }
            }
        }
    }

    @Test
    public void nullClobTest() throws SQLException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("select cast(? as clob)")) {
                stmt.setString(1, null);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    Clob clob = rs.getClob(1);
                    assertThat(clob).isNull();
                }
            }
        }
    }

    @Test
    public void github720_Incorrect_Update_Count_After_Deleting_Many_Rows() throws Exception {
        int size = 50000;
        Connection conn = getConnection();
        conn.createStatement().execute("drop table if exists test");
        conn.createStatement().execute("create table test (id int not null)");
        for (int i = 0; i < size; i++) {
            conn.createStatement().execute("insert into test values(" + i + ")");
        }
        int deletedCount = conn.createStatement().executeUpdate("delete from test");
        conn.close();

        assertThat(deletedCount).isEqualTo(size);
    }
}
