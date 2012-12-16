package org.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RSMetaDataTest
{
    private Connection conn;
    private Statement stat;
    private ResultSetMetaData meta;

    @BeforeClass
    public static void forName() throws Exception
    {
        Class.forName("org.sqlite.JDBC");
    }

    @Before
    public void connect() throws Exception
    {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
        stat.executeUpdate("create table People (pid integer primary key autoincrement, "
                + " firstname string, surname string, dob date);");
        stat.executeUpdate("insert into people values (null, 'Mohandas', 'Gandhi', " + " '1869-10-02');");
        meta = stat.executeQuery("select pid, firstname, surname from people;").getMetaData();
    }

    @After
    public void close() throws SQLException
    {
        stat.executeUpdate("drop table people;");
        stat.close();
        conn.close();
    }

    @Test
    public void catalogName() throws SQLException
    {
        assertEquals(meta.getCatalogName(1), "People");
    }

    @Test
    public void columns() throws SQLException
    {
        assertEquals(meta.getColumnCount(), 3);
        assertEquals(meta.getColumnName(1), "pid");
        assertEquals(meta.getColumnName(2), "firstname");
        assertEquals(meta.getColumnName(3), "surname");
        assertEquals(meta.getColumnType(1), Types.INTEGER);
        assertEquals(meta.getColumnType(2), Types.VARCHAR);
        assertEquals(meta.getColumnType(3), Types.VARCHAR);
        assertTrue(meta.isAutoIncrement(1));
        assertFalse(meta.isAutoIncrement(2));
        assertFalse(meta.isAutoIncrement(3));
        assertEquals(meta.isNullable(1), ResultSetMetaData.columnNoNulls);
        assertEquals(meta.isNullable(2), ResultSetMetaData.columnNullable);
        assertEquals(meta.isNullable(3), ResultSetMetaData.columnNullable);
    }

    @Test
    public void differentRS() throws SQLException
    {
        meta = stat.executeQuery("select * from people;").getMetaData();
        assertEquals(meta.getColumnCount(), 4);
        assertEquals(meta.getColumnName(1), "pid");
        assertEquals(meta.getColumnName(2), "firstname");
        assertEquals(meta.getColumnName(3), "surname");
        assertEquals(meta.getColumnName(4), "dob");
    }

    @Test
    public void nullable() throws SQLException
    {
        meta = stat.executeQuery("select null;").getMetaData();
        assertEquals(meta.isNullable(1), ResultSetMetaData.columnNullable);
    }

    @Test(expected = SQLException.class)
    public void badCatalogIndex() throws SQLException
    {
        meta.getCatalogName(4);
    }

    @Test(expected = SQLException.class)
    public void badColumnIndex() throws SQLException
    {
        meta.getColumnName(4);
    }

}
