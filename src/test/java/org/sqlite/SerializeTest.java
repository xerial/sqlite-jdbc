package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SerializeTest {

    private byte[] serialize() throws SQLException {
        try (SQLiteConnection connection =
                (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            execute(connection, "ATTACH ? AS ?", ":memory:", "a_schema");
            execute(connection, "CREATE TABLE a_schema.a_table (x integer)");
            execute(connection, "INSERT INTO a_schema.a_table (x) values (?)", 1007);
            return connection.serialize("a_schema");
        }
    }

    @Test
    public void testSerializeDeserialize() throws SQLException {
        byte[] bb = serialize();
        deserializeAndAssert(bb);
    }

    private void deserializeAndAssert(byte[] bb) throws SQLException {
        try (SQLiteConnection connection =
                (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            execute(connection, "ATTACH ? AS ?", ":memory:", "another_schema");
            connection.deserialize("another_schema", bb);
            assertThat(fetch(connection, "SELECT * FROM another_schema.a_table")).isEqualTo(1007);
        }
    }

    @Test
    public void testGrow() throws SQLException {
        byte[] bb = serialize();
        int size = bb.length;
        try (SQLiteConnection connection =
                (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            execute(connection, "ATTACH ? AS ?", ":memory:", "a_schema");
            connection.deserialize("a_schema", bb);
            connection.setAutoCommit(false);
            try (PreparedStatement ps =
                    prepare(connection, "INSERT INTO a_schema.a_table (x) values (?)", 1)) {
                for (int i = 0; i < 10000; ++i) {
                    ps.execute();
                }
            }
            connection.commit();
            bb = connection.serialize("a_schema");
            int newSize = bb.length;
            assertThat(newSize).isGreaterThan(size);
        }

        try (SQLiteConnection connection =
                (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            execute(connection, "ATTACH ? AS ?", ":memory:", "a_schema");
            connection.deserialize("a_schema", bb);
            assertThat(fetch(connection, "SELECT count(*) FROM a_schema.a_table")).isEqualTo(10001);
        }
    }

    @Test
    public void testMultiFirstTimeSerialize() throws SQLException {
        int size = -1;
        for (int i = 0; i < 1_000; ++i) {
            byte[] b = serialize();
            if (size != -1) {
                assertThat(b.length).isEqualTo(size);
            }
            size = b.length;
        }
    }

    @Test
    public void testErrorCorrupt() {
        byte[] bb = {1, 2, 3};
        assertThatThrownBy(() -> deserializeAndAssert(bb))
                .isInstanceOf(SQLiteException.class)
                .satisfies(
                        ex ->
                                assertThat(((SQLiteException) ex).getResultCode())
                                        .isEqualTo(SQLiteErrorCode.SQLITE_NOTADB));
    }

    @Test
    public void testMultiDeserialize() throws SQLException {
        byte[] bb = serialize();
        try (SQLiteConnection connection =
                (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            execute(connection, "ATTACH ? AS ?", ":memory:", "a_schema");
            connection.deserialize("a_schema", bb);
            for (int i = 0; i < 10; ++i) {
                connection.setAutoCommit(false);
                for (int j = 0; j < 10000; ++j) {
                    execute(connection, "INSERT INTO a_schema.a_table (x) values (?)", i);
                }
                connection.setAutoCommit(true);
                assertThat(fetch(connection, "SELECT COUNT(1) FROM a_schema.a_table"))
                        .isEqualTo(10001);
                connection.deserialize("a_schema", bb);
                assertThat(fetch(connection, "SELECT COUNT(1) FROM a_schema.a_table")).isEqualTo(1);
            }
        }
    }

    @Test
    public void testErrorNoSuchSchema() throws SQLException {
        byte[] bb = serialize();
        try (SQLiteConnection connection =
                (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            assertThatThrownBy(() -> connection.deserialize("a_schema", bb))
                    .isInstanceOf(SQLiteException.class)
                    .satisfies(
                            ex ->
                                    assertThat(((SQLiteException) ex).getResultCode())
                                            .isEqualTo(SQLiteErrorCode.SQLITE_ERROR));
        }
    }

    @Test
    public void testUseSeveralTimes() throws SQLException {
        byte[] bb = serialize();
        for (int i = 0; i < 10; ++i) {
            deserializeAndAssert(bb);
        }
    }

    @Test
    public void testBufferIsFileCompatible() throws SQLException, IOException {

        byte[] buff = serialize();
        File tmp = File.createTempFile("sqlite_test", ".db");
        Files.write(tmp.toPath(), buff);
        try (SQLiteConnection connection =
                (SQLiteConnection)
                        DriverManager.getConnection("jdbc:sqlite:" + tmp.getAbsolutePath())) {
            assertThat(fetch(connection, "SELECT * FROM main.a_table")).isEqualTo(1007);
        }
        assertThat(tmp.delete()).isTrue();
    }

    @Test
    public void testFileIsBufferCompatible() throws SQLException, IOException {
        File tmp = File.createTempFile("sqlite_test", ".db");
        try (SQLiteConnection connection =
                (SQLiteConnection)
                        DriverManager.getConnection("jdbc:sqlite:" + tmp.getAbsolutePath())) {
            execute(connection, "CREATE TABLE a_table (x integer)");
            execute(connection, "INSERT INTO a_table (x) values (?)", 1007);
        }
        byte[] buff = Files.readAllBytes(tmp.toPath());
        deserializeAndAssert(buff);
        assertThat(tmp.delete()).isTrue();
    }

    @Disabled("This takes around 15 seconds on a fast (2023) machine and consumes 4gb of memory")
    @Test
    public void testVeryLarge() throws SQLException {
        byte[] large;
        int rowCount;

        try (SQLiteConnection connection =
                (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            execute(connection, "ATTACH ? AS ?", ":memory:", "a_schema");
            connection.deserialize("a_schema", serialize());

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 500; ++i) { // we want this to be well withing a page
                sb.append("a");
            }
            String s = sb.toString();
            assertThatThrownBy(
                            () -> {
                                //noinspection InfiniteLoopStatement
                                while (true) {
                                    execute(
                                            connection,
                                            "INSERT INTO a_schema.a_table (x) values (?)",
                                            s);
                                }
                            })
                    .isInstanceOf(SQLiteException.class)
                    .satisfies(
                            ex ->
                                    assertThat(((SQLiteException) ex).getResultCode())
                                            .isEqualTo(SQLiteErrorCode.SQLITE_FULL));

            int pageSize = fetch(connection, "pragma a_schema.page_size");
            int pageCount = fetch(connection, "pragma a_schema.page_count");
            rowCount = fetch(connection, "SELECT COUNT(1) FROM a_schema.a_table");
            large = connection.serialize("a_schema");
            assertThat(large.length).isEqualTo(pageCount * pageSize);
            assertThat(large.length)
                    .isGreaterThan(2_000_000_000); // Too accurate, we risk pagesize change
        }

        try (SQLiteConnection connection =
                (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            execute(connection, "ATTACH ? AS ?", ":memory:", "a_schema");
            connection.deserialize("a_schema", large);
            int pageSize = fetch(connection, "pragma a_schema.page_size");
            int pageCount = fetch(connection, "pragma a_schema.page_count");
            assertThat(pageSize * pageCount).isEqualTo(large.length);
            assertThat(fetch(connection, "SELECT COUNT(1) FROM a_schema.a_table"))
                    .isEqualTo(rowCount);
        }
    }

    @Disabled("Use this for performance comparison and leak checks")
    @Test
    public void testPerformance() throws SQLException {
        byte[] buff = serialize();
        try (SQLiteConnection connection =
                (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            execute(connection, "ATTACH ? AS ?", ":memory:", "a_schema");
            for (int i = 0; i < 3_000_000; ++i) {
                connection.deserialize("a_schema", buff);
                buff = connection.serialize("a_schema");
            }
        }
    }

    private void execute(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = prepare(connection, sql, params)) {
            if (ps.execute()) {
                ps.getResultSet().close();
            }
        }
    }

    int fetch(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = prepare(connection, sql, params);
                ResultSet rs = ps.executeQuery()) {
            assertThat(rs.next()).isTrue();
            return rs.getInt(1);
        }
    }

    private PreparedStatement prepare(Connection connection, String sql, Object... params)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql);
        int i = 0;
        for (Object p : params) {
            ps.setObject(++i, p);
        }
        return ps;
    }
}
