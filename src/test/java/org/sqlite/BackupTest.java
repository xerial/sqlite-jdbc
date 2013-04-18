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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.BeforeClass;
import org.junit.Test;

public class BackupTest
{

    @BeforeClass
    public static void forName() throws Exception {
        Class.forName("org.sqlite.JDBC");
    }

    @Test
    public void backupAndRestore() throws SQLException, IOException {
        // create a memory database
        File tmpFile = File.createTempFile("backup-test", ".sqlite");
        tmpFile.deleteOnExit();

        // memory DB to file
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("create table sample(id, name)");
        stmt.executeUpdate("insert into sample values(1, \"leo\")");
        stmt.executeUpdate("insert into sample values(2, \"yui\")");

        stmt.executeUpdate("backup to " + tmpFile.getAbsolutePath());
        stmt.close();

        // open another memory database
        Connection conn2 = DriverManager.getConnection("jdbc:sqlite:");
        Statement stmt2 = conn2.createStatement();
        stmt2.execute("restore from " + tmpFile.getAbsolutePath());
        ResultSet rs = stmt2.executeQuery("select * from sample");
        int count = 0;
        while (rs.next()) {
            count++;
        }

        assertEquals(2, count);
        rs.close();

    }

    @Test
    public void memoryToDisk() throws Exception {

        if (!SQLiteJDBCLoader.isNativeMode())
            return; // skip this test in pure-java mode

        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("create table sample(id integer primary key autoincrement, name)");
        for (int i = 0; i < 10000; i++)
            stmt.executeUpdate("insert into sample(name) values(\"leo\")");

        File tmpFile = File.createTempFile("backup-test2", ".sqlite");
        tmpFile.deleteOnExit();
        //System.err.println("backup start");
        stmt.executeUpdate("backup to " + tmpFile.getAbsolutePath());
        stmt.close();
        //System.err.println("backup done.");

    }

}
