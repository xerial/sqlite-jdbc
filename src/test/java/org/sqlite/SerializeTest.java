package org.sqlite;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.sql.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


public class SerializeTest {

    private ByteBuffer serialize() throws SQLException {
        try (SQLiteConnection connection = (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            execute(connection, "ATTACH ? AS ?", ":memory:", "a_schema");
            execute(connection, "CREATE TABLE a_schema.a_table (x integer)");
            execute(connection, "INSERT INTO a_schema.a_table (x) values (?)", 1007);
            return connection.serialize("a_schema");
        }
    }

    @Test
    public void testSerializeDeserialize() throws SQLException {
        ByteBuffer bb = serialize();
        deserializeAndAssert(bb);
    }

    private void deserializeAndAssert(ByteBuffer bb) throws SQLException {
        try (SQLiteConnection connection = (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            execute(connection, "ATTACH ? AS ?", ":memory:", "another_schema");
            connection.deserialize("another_schema", bb);
            assertThat(fetch(connection, "SELECT * FROM another_schema.a_table")).isEqualTo(1007);
        }
    }

    @Test
    public void testSerializeByteArrayDeserialize() throws SQLException {
        ByteBuffer bb = serialize();
        assertThat(bb.isDirect()).isTrue();
        byte[] buff = new byte[bb.remaining()];
        bb.get(buff);
        try {
            deserializeAndAssert(ByteBuffer.wrap(buff));
            fail("exception expected");
        } catch (IllegalArgumentException ignore) {
            // has to be a direct buffer
        }

        bb = ByteBuffer.allocateDirect(buff.length);
        bb.put(buff);
        deserializeAndAssert(bb);
    }

    @Test
    public void testGrow() throws SQLException {
        ByteBuffer bb = serialize();
        int size = bb.remaining();
        try (SQLiteConnection connection = (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            execute(connection, "ATTACH ? AS ?", ":memory:", "a_schema");
            connection.deserialize("a_schema", bb);
            connection.setAutoCommit(false);
            try (PreparedStatement ps = prepare(connection, "INSERT INTO a_schema.a_table (x) values (?)", 1)) {
                for (int i=0;i<10000;++i) {
                    ps.execute();
                }
            }
            connection.commit();
            bb = connection.serialize("a_schema");
            int newSize = bb.remaining();
            assertThat(newSize).isGreaterThan(size);
        }

        try (SQLiteConnection connection = (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            execute(connection, "ATTACH ? AS ?", ":memory:", "a_schema");
            connection.deserialize("a_schema", bb);
            assertThat(fetch(connection, "SELECT count(*) FROM a_schema.a_table")).isEqualTo(10001);
        }
    }

    @Test
    public void testMultiFirstTimeSerialize() throws SQLException {
        for (int i=0;i<1_000;++i) {
            serialize();
        }
    }

    @Test
    public void testErrorCorrupt() {
        ByteBuffer bb = ByteBuffer.allocateDirect(3);
        bb.put((byte)1);
        bb.put((byte)2);
        bb.put((byte)3);
        try {
            deserializeAndAssert(bb);
            fail("exception expected");
        } catch (SQLException e) {
            assertThat(e.getErrorCode()).isEqualTo(SQLiteErrorCode.SQLITE_NOTADB.code);
        }
    }

    @Test
    public void testMultiDeserialize() throws SQLException {
        ByteBuffer bb = serialize();
        try (SQLiteConnection connection = (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            execute(connection, "ATTACH ? AS ?", ":memory:", "a_schema");
            connection.deserialize("a_schema", bb);
            for (int i=0;i<10;++i) {
                connection.setAutoCommit(false);
                for (int j=0;j<10000;++j) {
                    execute(connection, "INSERT INTO a_schema.a_table (x) values (?)", i);
                }
                connection.setAutoCommit(true);
                assertThat(fetch(connection, "SELECT COUNT(1) FROM a_schema.a_table")).isEqualTo(10001);
                connection.deserialize("a_schema", bb);
                assertThat(fetch(connection, "SELECT COUNT(1) FROM a_schema.a_table")).isEqualTo(1);
            }
        }
    }

    @Test
    public void testErrorNoSuchSchema() throws SQLException {
        ByteBuffer bb = serialize();
        try (SQLiteConnection connection = (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            connection.deserialize("a_schema", bb);
            fail("exception expected");
        } catch (SQLiteException e) {
            assertThat(e.getResultCode()).isEqualTo(SQLiteErrorCode.SQLITE_ERROR);
        }
    }

    @Test
    public void testUseSeveralTimes() throws SQLException {
        ByteBuffer bb = serialize();
        for (int i=0;i<10;++i) {
            deserializeAndAssert(bb);
        }
    }

    @Test
    public void testSize() throws SQLException {
        try (SQLiteConnection connection = (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:")) {
            execute(connection, "ATTACH ? AS ?", ":memory:", "a_schema");
            execute(connection, "CREATE TABLE a_schema.a_table (x integer)");
            execute(connection, "INSERT INTO a_schema.a_table (x) values (?)", 1007);
            int pageSize = fetch(connection, "pragma a_schema.page_size");
            int pageCount = fetch(connection, "pragma a_schema.page_count");
            assertThat(connection.serializeSize("a_schema")).isEqualTo((long)pageSize * pageCount);
        }
    }

    private void execute(Connection connection, String sql, Object...params) throws SQLException {
        try (PreparedStatement ps = prepare(connection, sql, params)) {
            if (ps.execute()) {
                ps.getResultSet().close();
            }
        }
    }

    int fetch(Connection connection, String sql, Object...params) throws SQLException {
        try (PreparedStatement ps = prepare(connection, sql, params);
             ResultSet rs = ps.executeQuery()) {
            assertThat(rs.next()).isTrue();
            return rs.getInt(1);
        }
    }

    private PreparedStatement prepare(Connection connection, String sql, Object ... params) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql);
        int i = 0;
        for (Object p : params) {
            ps.setObject(++i, p);
        }
        return ps;
    }
}
