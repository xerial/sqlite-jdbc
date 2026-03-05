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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    @ParameterizedTest
    @MethodSource
    public void removeQuotation(String input, String expected) throws SQLException {
        assertThat(ExtendedCommand.removeQuotation(input)).isEqualTo(expected);
    }

    private static Stream<Arguments> removeQuotation() {
        return Stream.of(
                Arguments.of(null, null), // Null String
                Arguments.of("'", "'"), // String with one single quotation only
                Arguments.of("\"", "\""), // String with one double quotation only
                Arguments.of("'Test\"", "'Test\""), // String with two mismatch quotations
                Arguments.of("'Test'", "Test"), // String with two matching single quotations
                Arguments.of("\"Test\"", "Test"), // String with two matching double quotations
                Arguments.of("'Te's\"t'", "Te's\"t") // String with more than two quotations
                );
    }
}
