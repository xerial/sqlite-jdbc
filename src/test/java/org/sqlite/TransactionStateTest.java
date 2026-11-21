package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.sqlite.core.Codes.SQLITE_TXN_NONE;
import static org.sqlite.core.Codes.SQLITE_TXN_READ;
import static org.sqlite.core.Codes.SQLITE_TXN_WRITE;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.core.DB;

public class TransactionStateTest {
    @Test
    public void followsReadWriteAndRollback() throws SQLException {
        try (SQLiteConnection connection =
                        (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:");
                Statement statement = connection.createStatement()) {
            DB db = connection.getDatabase();
            statement.executeUpdate("CREATE TABLE test(value)");
            statement.executeUpdate("INSERT INTO test VALUES(10)");
            assertThat(db.transactionState("main")).isEqualTo(SQLITE_TXN_NONE);

            connection.setAutoCommit(false);
            assertThat(db.transactionState("main")).isEqualTo(SQLITE_TXN_NONE);
            try (ResultSet result = statement.executeQuery("SELECT value FROM test")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(10);
            }
            assertThat(db.transactionState("main")).isEqualTo(SQLITE_TXN_READ);

            statement.executeUpdate("UPDATE test SET value = 20");
            assertThat(db.transactionState("main")).isEqualTo(SQLITE_TXN_WRITE);
            connection.rollback();
            assertThat(db.transactionState("main")).isEqualTo(SQLITE_TXN_NONE);
            try (ResultSet result = statement.executeQuery("SELECT value FROM test")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(10);
            }
            connection.commit();
            assertThat(db.transactionState("main")).isEqualTo(SQLITE_TXN_NONE);
        }
    }

    @Test
    public void distinguishesSchemasAndReturnsHighestStateForNull() throws SQLException {
        String schema = "attached_\uD83D\uDE80";
        try (SQLiteConnection connection =
                        (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:");
                Statement statement = connection.createStatement()) {
            DB db = connection.getDatabase();
            try (PreparedStatement attach = connection.prepareStatement("ATTACH ? AS ?")) {
                attach.setString(1, ":memory:");
                attach.setString(2, schema);
                attach.execute();
            }
            statement.executeUpdate("CREATE TABLE main.test(value)");
            statement.executeUpdate("INSERT INTO main.test VALUES(10)");
            statement.executeUpdate("CREATE TABLE \"" + schema + "\".test(value)");
            assertThat(db.transactionState(null)).isEqualTo(SQLITE_TXN_NONE);

            connection.setAutoCommit(false);
            try (ResultSet result = statement.executeQuery("SELECT value FROM main.test")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(10);
            }
            statement.executeUpdate("INSERT INTO \"" + schema + "\".test VALUES(20)");
            assertThat(db.transactionState("main")).isEqualTo(SQLITE_TXN_READ);
            assertThat(db.transactionState(schema)).isEqualTo(SQLITE_TXN_WRITE);
            assertThat(db.transactionState(null)).isEqualTo(SQLITE_TXN_WRITE);
            assertThat(db.transactionState("missing")).isEqualTo(-1);
            assertThat(db.transactionState("")).isEqualTo(-1);

            connection.commit();
            assertThat(db.transactionState("main")).isEqualTo(SQLITE_TXN_NONE);
            assertThat(db.transactionState(schema)).isEqualTo(SQLITE_TXN_NONE);
            assertThat(db.transactionState(null)).isEqualTo(SQLITE_TXN_NONE);
        }
    }

    @Test
    public void rejectsClosedDatabase() throws SQLException {
        DB db;
        try (SQLiteConnection connection =
                (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            db = connection.getDatabase();
        }
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> db.transactionState(null))
                .withMessageContaining("closed");
    }

    @Test
    public void reportsStateDuringWriteContention(@TempDir Path directory) throws SQLException {
        String url = "jdbc:sqlite:" + directory.resolve("transactions.db");
        try (SQLiteConnection first = (SQLiteConnection) DriverManager.getConnection(url);
                SQLiteConnection second = (SQLiteConnection) DriverManager.getConnection(url);
                Statement firstStatement = first.createStatement();
                Statement secondStatement = second.createStatement()) {
            firstStatement.executeUpdate("CREATE TABLE test(value)");
            firstStatement.executeUpdate("INSERT INTO test VALUES(10)");
            first.setAutoCommit(false);
            second.setAutoCommit(false);
            second.setBusyTimeout(0);
            try (ResultSet firstResult = firstStatement.executeQuery("SELECT value FROM test");
                    ResultSet secondResult =
                            secondStatement.executeQuery("SELECT value FROM test")) {
                assertThat(firstResult.next()).isTrue();
                assertThat(secondResult.next()).isTrue();
                assertThat(firstResult.getInt(1)).isEqualTo(10);
                assertThat(secondResult.getInt(1)).isEqualTo(10);
            }

            firstStatement.executeUpdate("UPDATE test SET value = 20");
            assertThatExceptionOfType(SQLiteException.class)
                    .isThrownBy(() -> secondStatement.executeUpdate("UPDATE test SET value = 30"))
                    .satisfies(
                            error ->
                                    assertThat(error.getResultCode())
                                            .isEqualTo(SQLiteErrorCode.SQLITE_BUSY));
            assertThat(first.getDatabase().transactionState("main")).isEqualTo(SQLITE_TXN_WRITE);
            assertThat(second.getDatabase().transactionState("main")).isEqualTo(SQLITE_TXN_READ);

            first.rollback();
            second.rollback();
            assertThat(first.getDatabase().transactionState(null)).isEqualTo(SQLITE_TXN_NONE);
            assertThat(second.getDatabase().transactionState(null)).isEqualTo(SQLITE_TXN_NONE);
        }
    }
}
