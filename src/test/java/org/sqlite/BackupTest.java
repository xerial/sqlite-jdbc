//--------------------------------------
// sqlite-jdbc Project
//
// BackupTest.java
// Since: Feb 18, 2009
//
// $URL$ 
// $Author$
//--------------------------------------
package org.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.BeforeClass;
import org.junit.Test;

public class BackupTest
{
    @BeforeClass
    public static void forName() throws Exception
    {
        Class.forName("org.sqlite.JDBC");
    }

    @Test
    public void openMemory() throws SQLException
    {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("create table sample(id, name)");
        stmt.executeUpdate("insert into sample values(1, \"leo\")");
        stmt.executeUpdate("insert into sample values(2, \"yui\")");

        // TODO some backup mechanism 
        // stmt.executeUpdate("backup sample.db");
        conn.close();
    }

}
