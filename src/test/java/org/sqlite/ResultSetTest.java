package org.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ResultSetTest {

    private Connection conn;
    private Statement stat;

    @Before
    public void connect() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
        stat.executeUpdate("create table test (id int primary key, DESCRIPTION varchar(40), fOo varchar(3));");
        stat.executeUpdate("insert into test values (1, 'description', 'bar')");
    }

    @After
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
    @Test(expected = SQLException.class)
    public void testSelectWithTableNameAliasNowNotFindWithTableNameAlias()
            throws SQLException {
        ResultSet resultSet = stat.executeQuery("select t.id from test as t");
        assertTrue(resultSet.next());
        resultSet.findColumn("t.id");
    }

    @Test
    public void testSelectWithTableNameNowFindWithoutTableName()
            throws SQLException {
        ResultSet resultSet = stat.executeQuery("select test.id from test");
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.findColumn("id"));
    }

    @Test(expected = SQLException.class)
    public void testSelectWithTableNameNowNotFindWithTableName()
            throws SQLException {
        ResultSet resultSet = stat.executeQuery("select test.id from test");
        assertTrue(resultSet.next());
        resultSet.findColumn("test.id");
    }

}
