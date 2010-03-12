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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;

import org.junit.BeforeClass;
import org.junit.Test;

public class BackupTest
{
    @BeforeClass
    public static void forName() throws Exception {
        Class.forName("org.sqlite.JDBC");
    }

    @Test
    public void backup() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("create table sample(id, name)");
        stmt.executeUpdate("insert into sample values(1, \"leo\")");
        stmt.executeUpdate("insert into sample values(2, \"yui\")");

        // TODO some backup mechanism 
        stmt.executeUpdate("backup to target/sample.db");
        conn.close();
    }

    @Test
    public void parseBackupCmd() {
        Matcher m = Stmt.parseBackupCommand("backup mydb to somewhere/backupfolder/mydb.sqlite");
        assertTrue(m.matches());
        assertEquals("mydb", m.group(2));
        assertEquals("somewhere/backupfolder/mydb.sqlite", m.group(3));

        m = Stmt.parseBackupCommand("backup main to \"tmp folder with space\"");
        assertTrue(m.matches());
        assertEquals("main", m.group(2));
        assertEquals("\"tmp folder with space\"", m.group(3));

        m = Stmt.parseBackupCommand("backup to target/sample.db");
        assertTrue(m.matches());
        assertEquals("target/sample.db", m.group(3));

    }

}
