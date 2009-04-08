//--------------------------------------
// sqlite-jdbc Project
//
// JDBCTest.java
// Since: Apr 8, 2009
//
// $URL$ 
// $Author$
//--------------------------------------
package org.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

public class JDBCTest
{
    @BeforeClass
    public static void forName() throws Exception
    {
        Class.forName("org.sqlite.JDBC");
    }

    @Test
    public void enableLoadExtensionTest() throws Exception
    {
        Properties prop = new Properties();
        prop.setProperty("enable_load_extension", "true");

        Connection conn = null;
        try
        {
            conn = DriverManager.getConnection("jdbc:sqlite:", prop);
            Statement stat = conn.createStatement();
            stat.executeUpdate("create table test (id integer primary key, fn, sn);");
            stat.executeUpdate("create view testView as select * from test;");
        }
        finally
        {
            if (conn != null)
                conn.close();
        }
    }

}
