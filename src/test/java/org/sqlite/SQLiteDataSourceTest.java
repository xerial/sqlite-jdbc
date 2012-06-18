//--------------------------------------
// sqlite-jdbc Project
//
// SQLiteDataSourceTest.java
// Since: Mar 11, 2010
//
// $URL$ 
// $Author$
//--------------------------------------
package org.sqlite;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SQLiteDataSourceTest
{

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void enumParam() throws Exception {

        SQLiteDataSource ds = new SQLiteDataSource();
        Connection conn = ds.getConnection();
        Statement stat = conn.createStatement();
        try {

            stat.executeUpdate("create table A (id integer, name)");
            stat.executeUpdate("insert into A values(1, 'leo')");
            ResultSet rs = stat.executeQuery("select * from A");
            int count = 0;
            while (rs.next()) {
                count++;
                int id = rs.getInt(1);
                String name = rs.getString(2);
                assertEquals(1, id);
                assertEquals("leo", name);
            }
            assertEquals(1, count);

        }
        finally {
            stat.close();
            conn.close();
        }

    }

    @Test
    public void encoding() throws Exception {

        String[] configArray = new String[] {
                "UTF8", "UTF-8", "UTF_8",
                "UTF16", "UTF-16", "UTF_16",
                "UTF_16LE", "UTF-16LE", "UTF16_LITTLE_ENDIAN",
                "UTF_16BE", "UTF-16BE","UTF16_BIG_ENDIAN" };
        String[] encodingArray = new String[] {
                "UTF-8", "UTF-16le", "UTF-16le", "UTF-16be" };

        for (int i = 0; i < configArray.length; i++) {
            SQLiteDataSource ds = new SQLiteDataSource();
            ds.setEncoding(configArray[i]);

            Connection conn = ds.getConnection();
            Statement stat = conn.createStatement();
            try {

                ResultSet rs = stat.executeQuery("pragma encoding");
                assertEquals(encodingArray[i / 3], rs.getString(1));
            } finally {
                stat.close();
                conn.close();
            }
        }
    }
}
