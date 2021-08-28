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

import org.junit.jupiter.api.Test;
import org.sqlite.ExtendedCommand.BackupCommand;
import org.sqlite.ExtendedCommand.RestoreCommand;
import org.sqlite.ExtendedCommand.SQLExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExtendedCommandTest {

    public static BackupCommand parseBackupCommand(String sql) throws SQLException {
        SQLExtension e = ExtendedCommand.parse(sql);
        assertTrue(e instanceof BackupCommand);
        return (BackupCommand) e;
    }

    public static RestoreCommand parseRestoreCommand(String sql) throws SQLException {
        SQLExtension e = ExtendedCommand.parse(sql);
        assertTrue(e instanceof RestoreCommand);
        return (RestoreCommand) e;
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
