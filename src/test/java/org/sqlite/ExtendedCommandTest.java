// --------------------------------------
// sqlite-jdbc Project
//
// ExtendedCommandTest.java
// Since: Mar 12, 2010
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.sqlite.ExtendedCommand.BackupCommand;
import org.sqlite.ExtendedCommand.RestoreCommand;
import org.sqlite.ExtendedCommand.SQLExtension;

public class ExtendedCommandTest {

    public static BackupCommand parseBackupCommand(String sql) throws SQLException {
        SQLExtension e = ExtendedCommand.parse(sql);
        assertThat(e instanceof BackupCommand).isTrue();
        return (BackupCommand) e;
    }

    public static RestoreCommand parseRestoreCommand(String sql) throws SQLException {
        SQLExtension e = ExtendedCommand.parse(sql);
        assertThat(e instanceof RestoreCommand).isTrue();
        return (RestoreCommand) e;
    }

    @Test
    public void parseBackupCmd() throws SQLException {
        BackupCommand b = parseBackupCommand("backup mydb to somewhere/backupfolder/mydb.sqlite");
        assertThat(b.srcDB).isEqualTo("mydb");
        assertThat(b.destFile).isEqualTo("somewhere/backupfolder/mydb.sqlite");

        b = parseBackupCommand("backup main to \"tmp folder with space\"");
        assertThat(b.srcDB).isEqualTo("main");
        assertThat(b.destFile).isEqualTo("tmp folder with space");

        b = parseBackupCommand("backup main to 'tmp folder with space'");
        assertThat(b.srcDB).isEqualTo("main");
        assertThat(b.destFile).isEqualTo("tmp folder with space");

        b = parseBackupCommand("backup to target/sample.db");
        assertThat(b.srcDB).isEqualTo("main");
        assertThat(b.destFile).isEqualTo("target/sample.db");
    }

    @Test
    public void parseRestoreCmd() throws SQLException {
        RestoreCommand b =
                parseRestoreCommand("restore mydb from somewhere/backupfolder/mydb.sqlite");
        assertThat(b.targetDB).isEqualTo("mydb");
        assertThat(b.srcFile).isEqualTo("somewhere/backupfolder/mydb.sqlite");

        b = parseRestoreCommand("restore main from \"tmp folder with space\"");
        assertThat(b.targetDB).isEqualTo("main");
        assertThat(b.srcFile).isEqualTo("tmp folder with space");

        b = parseRestoreCommand("restore main from 'tmp folder with space'");
        assertThat(b.targetDB).isEqualTo("main");
        assertThat(b.srcFile).isEqualTo("tmp folder with space");

        b = parseRestoreCommand("restore from target/sample.db");
        assertThat(b.targetDB).isEqualTo("main");
        assertThat(b.srcFile).isEqualTo("target/sample.db");
    }
}
