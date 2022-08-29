package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResultSetTest {

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
    public void testTableColumnLowerNowFindLowerCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("id")).isEqualTo(1);
    }

    @Test
    public void testTableColumnLowerNowFindUpperCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("ID")).isEqualTo(1);
    }

    @Test
    public void testTableColumnLowerNowFindMixedCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("Id")).isEqualTo(1);
    }

    @Test
    public void testTableColumnUpperNowFindLowerCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("description")).isEqualTo(2);
    }

    @Test
    public void testTableColumnUpperNowFindUpperCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("DESCRIPTION")).isEqualTo(2);
    }

    @Test
    public void testTableColumnUpperNowFindMixedCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("Description")).isEqualTo(2);
    }

    @Test
    public void testTableColumnMixedNowFindLowerCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("foo")).isEqualTo(3);
    }

    @Test
    public void testTableColumnMixedNowFindUpperCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("FOO")).isEqualTo(3);
    }

    @Test
    public void testTableColumnMixedNowFindMixedCaseColumn() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("fOo")).isEqualTo(3);
    }

    @Test
    public void testSelectWithTableNameAliasNowFindWithoutTableNameAlias() throws SQLException {
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
    public void testSelectWithTableNameAliasNowNotFindWithTableNameAlias() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select t.id from test as t");
        assertThat(resultSet.next()).isTrue();
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> resultSet.findColumn("t.id"));
    }

    @Test
    public void testSelectWithTableNameNowFindWithoutTableName() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select test.id from test");
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.findColumn("id")).isEqualTo(1);
    }

    @Test
    public void testSelectWithTableNameNowNotFindWithTableName() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select test.id from test");
        assertThat(resultSet.next()).isTrue();
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> resultSet.findColumn("test.id"));
    }

    @Test
    public void testCloseStatement() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select test.id from test");

        stat.close();

        assertThat(stat.isClosed()).isTrue();
        assertThat(resultSet.isClosed()).isTrue();

        resultSet.close();

        assertThat(resultSet.isClosed()).isTrue();
    }

    @Test
    public void testReturnsNonAsciiCodepoints() throws SQLException {
        String nonAsciiString = "국정의 중요한 사항에 관한";
        PreparedStatement pstat = conn.prepareStatement("select ?");
        pstat.setString(1, nonAsciiString);

        ResultSet resultSet = pstat.executeQuery();

        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.getString(1)).isEqualTo(nonAsciiString);
        assertThat(resultSet.next()).isFalse();
    }

    @Test
    public void testFindColumnOnEmptyResultSet() throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test where id = 0");
        assertThat(resultSet.next()).isFalse();
        assertThat(resultSet.findColumn("id")).isEqualTo(1);
    }
}
