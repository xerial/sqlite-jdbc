//--------------------------------------
// sqlite-jdbc Project
//
// ExtendedCommandTest.java
// Since: Mar 12, 2010
//
// $URL$ 
// $Author$
//--------------------------------------
package org.sqlite;

import static org.junit.Assert.*;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sqlite.ExtendedCommand.BackupCommand;
import org.sqlite.ExtendedCommand.RestoreCommand;
import org.sqlite.ExtendedCommand.SQLExtension;

public class ExtendedCommandTest
{

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    public static BackupCommand parseBackupCommand(String sql) throws SQLException {
        SQLExtension e = ExtendedCommand.parse(sql);
        assertTrue(BackupCommand.class.isInstance(e));
        return BackupCommand.class.cast(e);
    }

    public static RestoreCommand parseRestoreCommand(String sql) throws SQLException {
        SQLExtension e = ExtendedCommand.parse(sql);
        assertTrue(RestoreCommand.class.isInstance(e));
        return RestoreCommand.class.cast(e);
    }

    @Test
    public void parseBackupCmd() throws SQLException {
        BackupCommand b = parseBackupCommand("backup mydb to somewhere/backupfolder/mydb.sqlite");
        assertEquals("mydb", b.srcDB);
        assertEquals("somewhere/backupfolder/mydb.sqlite", b.destFile);

        b = parseBackupCommand("backup main to \"tmp folder with space\"");
        assertEquals("main", b.srcDB);
        assertEquals("tmp folder with space", b.destFile);

        b = parseBackupCommand("backup main to 'tmp folder with space'");
        assertEquals("main", b.srcDB);
        assertEquals("tmp folder with space", b.destFile);

        b = parseBackupCommand("backup to target/sample.db");
        assertEquals("main", b.srcDB);
        assertEquals("target/sample.db", b.destFile);
    }

    @Test
    public void parseRestoreCmd() throws SQLException {
        RestoreCommand b = parseRestoreCommand("restore mydb from somewhere/backupfolder/mydb.sqlite");
        assertEquals("mydb", b.targetDB);
        assertEquals("somewhere/backupfolder/mydb.sqlite", b.srcFile);

        b = parseRestoreCommand("restore main from \"tmp folder with space\"");
        assertEquals("main", b.targetDB);
        assertEquals("tmp folder with space", b.srcFile);

        b = parseRestoreCommand("restore main from 'tmp folder with space'");
        assertEquals("main", b.targetDB);
        assertEquals("tmp folder with space", b.srcFile);

        b = parseRestoreCommand("restore from target/sample.db");
        assertEquals("main", b.targetDB);
        assertEquals("target/sample.db", b.srcFile);
    }

}
