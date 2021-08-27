package org.sqlite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResultSetTest {

    private Connection conn;
    private Statement stat;

    @BeforeEach
    public void connect() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
        stat.executeUpdate("create table test (id int primary key, DESCRIPTION varchar(40), fOo varchar(3));");
        stat.executeUpdate("insert into test values (1, 'description', 'bar')");
    }

    @AfterEach
    public void close() throws SQLException {
        stat.close();
        conn.close();
    }

    @Test
    public void testTableColumnLowerNowFindLowerCaseColumn()
        throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.findColumn("id"));
    }

    @Test
    public void testTableColumnLowerNowFindUpperCaseColumn()
        throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.findColumn("ID"));
    }

    @Test
    public void testTableColumnLowerNowFindMixedCaseColumn()
        throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.findColumn("Id"));
    }

    @Test
    public void testTableColumnUpperNowFindLowerCaseColumn()
        throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertTrue(resultSet.next());
        assertEquals(2, resultSet.findColumn("description"));
    }

    @Test
    public void testTableColumnUpperNowFindUpperCaseColumn()
        throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertTrue(resultSet.next());
        assertEquals(2, resultSet.findColumn("DESCRIPTION"));
    }

    @Test
    public void testTableColumnUpperNowFindMixedCaseColumn()
        throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertTrue(resultSet.next());
        assertEquals(2, resultSet.findColumn("Description"));
    }

    @Test
    public void testTableColumnMixedNowFindLowerCaseColumn()
        throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertTrue(resultSet.next());
        assertEquals(3, resultSet.findColumn("foo"));
    }

    @Test
    public void testTableColumnMixedNowFindUpperCaseColumn()
        throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertTrue(resultSet.next());
        assertEquals(3, resultSet.findColumn("FOO"));
    }

    @Test
    public void testTableColumnMixedNowFindMixedCaseColumn()
        throws SQLException {
        ResultSet resultSet = stat.executeQuery("select * from test");
        assertTrue(resultSet.next());
        assertEquals(3, resultSet.findColumn("fOo"));
    }

    @Test
    public void testSelectWithTableNameAliasNowFindWithoutTableNameAlias()
        throws SQLException {
        ResultSet resultSet = stat.executeQuery("select t.id from test as t");
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.findColumn("id"));
    }

    /**
     * Can't produce a case where column name contains table name
     * https://www.sqlite.org/c3ref/column_name.html :
     * "If there is no AS clause then the name of the column is unspecified"
     */
    @Test
    public void testSelectWithTableNameAliasNowNotFindWithTableNameAlias()
        throws SQLException {
        ResultSet resultSet = stat.executeQuery("select t.id from test as t");
        assertTrue(resultSet.next());
        assertThrows(SQLException.class, () -> resultSet.findColumn("t.id"));
    }

    @Test
    public void testSelectWithTableNameNowFindWithoutTableName()
        throws SQLException {
        ResultSet resultSet = stat.executeQuery("select test.id from test");
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.findColumn("id"));
    }

    @Test
    public void testSelectWithTableNameNowNotFindWithTableName()
        throws SQLException {
        ResultSet resultSet = stat.executeQuery("select test.id from test");
        assertTrue(resultSet.next());
        assertThrows(SQLException.class, () -> resultSet.findColumn("test.id"));
    }

    @Test
    public void testCloseStatement()
        throws SQLException {
        ResultSet resultSet = stat.executeQuery("select test.id from test");

        stat.close();

        assertTrue(stat.isClosed());
        assertTrue(resultSet.isClosed());

        resultSet.close();

        assertTrue(resultSet.isClosed());
    }

    @Test
    public void testReturnsNonAsciiCodepoints()
        throws SQLException {
        String nonAsciiString = "국정의 중요한 사항에 관한";
        PreparedStatement pstat = conn.prepareStatement("select ?");
        pstat.setString(1, nonAsciiString);

        ResultSet resultSet = pstat.executeQuery();

        assertTrue(resultSet.next());
        assertEquals(nonAsciiString, resultSet.getString(1));
        assertFalse(resultSet.next());
    }
}
