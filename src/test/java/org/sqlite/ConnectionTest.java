package org.sqlite;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteConfig.Pragma;
import org.sqlite.SQLiteConfig.SynchronousMode;

/**
 * These tests check whether access to files is working correctly and some Connection.close() cases.
 */
public class ConnectionTest {

    @TempDir static File tempDir;

    @Test
    public void isValid() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        assertThat(conn.isValid(0)).isTrue();
        conn.close();
        assertThat(conn.isValid(0)).isFalse();
    }

    @Test
    public void executeUpdateOnClosedDB() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        Statement stat = conn.createStatement();
        conn.close();

        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> stat.executeUpdate("create table A(id, name)"));
    }

    @Test
    public void readOnly() throws SQLException {

        // set read only mode
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:", config.toProperties())) {
            try (Statement stat = conn.createStatement()) {
                assertThat(conn.isReadOnly()).isTrue();

                // these updates must be forbidden in read-only mode
                assertThatThrownBy(
                                () -> {
                                    stat.executeUpdate("create table A(id, name)");
                                    stat.executeUpdate("insert into A values(1, 'leo')");
                                })
                        .isInstanceOf(SQLException.class);
            }
            conn.close();

            config.setReadOnly(true); // should be a no-op

            assertThatThrownBy(() -> conn.setReadOnly(false))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining(
                            "Cannot change read-only flag after establishing a connection.");
        }
    }

    @Test
    public void foreignKeys() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:", config.toProperties());
                Statement stat = conn.createStatement()) {

            stat.executeUpdate(
                    "create table track(id integer primary key, name, aid, foreign key (aid) references artist(id))");
            stat.executeUpdate("create table artist(id integer primary key, name)");

            stat.executeUpdate("insert into artist values(10, 'leo')");
            stat.executeUpdate("insert into track values(1, 'first track', 10)"); // OK

            // invalid reference - detect violation of foreign key constraints
            assertThatExceptionOfType(SQLException.class)
                    .isThrownBy(
                            () ->
                                    stat.executeUpdate(
                                            "insert into track values(2, 'second track', 3)"));
        }
    }

    @Test
    public void canWrite() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:", config.toProperties())) {
            conn.createStatement();
            assertThat(conn.isReadOnly()).isFalse();
        }
    }

    @Test
    public void synchronous() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.setSynchronous(SynchronousMode.OFF);
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:", config.toProperties());
                Statement stat = conn.createStatement()) {
            ResultSet rs = stat.executeQuery("pragma synchronous");
            if (rs.next()) {
                ResultSetMetaData rm = rs.getMetaData();
                rm.getColumnCount();
                int synchronous = rs.getInt(1);
                assertThat(synchronous).isEqualTo(0);
            }
        }
    }

    @Test
    public void openMemory() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        conn.close();
    }

    @Test
    public void isClosed() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        conn.close();
        assertThat(conn.isClosed()).isTrue();
    }

    @Test
    public void closeTest() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        PreparedStatement prep = conn.prepareStatement("select null;");
        prep.executeQuery();
        conn.close();
        assertThatExceptionOfType(SQLException.class).isThrownBy(prep::clearParameters);
    }

    @Test
    public void openInvalidLocation() {
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> DriverManager.getConnection("jdbc:sqlite:/"));
    }

    @Test
    public void openResource() throws Exception {
        File testDB = copyToTemp("sample.db");
        assertThat(testDB.exists()).isTrue();
        Connection conn =
                DriverManager.getConnection(
                        String.format("jdbc:sqlite::resource:%s", testDB.toURI().toURL()));
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("select * from coordinate");
        assertThat(rs.next()).isTrue();
        rs.close();
        stat.close();
        conn.close();
    }

    @Test
    public void openJARResource() throws Exception {
        File testJAR = copyToTemp("testdb.jar");
        assertThat(testJAR.exists()).isTrue();

        Connection conn =
                DriverManager.getConnection(
                        String.format(
                                "jdbc:sqlite::resource:jar:%s!/sample.db",
                                testJAR.toURI().toURL()));
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("select * from coordinate");
        assertThat(rs.next()).isTrue();
        rs.close();
        stat.close();
        conn.close();
    }

    @Test
    public void openFile() throws Exception {
        File testDB = copyToTemp("sample.db");

        assertThat(testDB.exists()).isTrue();
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", testDB));
        conn.close();
    }

    @Test
    public void concurrentClose() throws SQLException, InterruptedException, ExecutionException {
        final Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        ResultSet[] rss = new ResultSet[512];
        for (int i = 0; i < rss.length; i++) {
            rss[i] = conn.prepareStatement("select null;").executeQuery();
        }
        ExecutorService finalizer = Executors.newSingleThreadExecutor();
        try {
            ArrayList<Future<Void>> futures = new ArrayList<>(rss.length);
            for (final ResultSet rs : rss) {
                futures.add(
                        finalizer.submit(
                                () -> {
                                    rs.close();
                                    return null;
                                }));
            }
            conn.close();
            for (Future<Void> f : futures) f.get();
        } finally {
            finalizer.shutdown();
        }
    }

    public static File copyToTemp(String fileName) throws IOException {
        InputStream in = ConnectionTest.class.getResourceAsStream(fileName);

        File tmp = File.createTempFile(fileName, "", tempDir);
        tmp.deleteOnExit();
        FileOutputStream out = new FileOutputStream(tmp);

        byte[] buf = new byte[8192];
        for (int readBytes; (readBytes = in.read(buf)) != -1; ) {
            out.write(buf, 0, readBytes);
        }
        out.flush();
        out.close();
        in.close();

        return tmp;
    }

    @Test
    public void URIFilenames() throws SQLException {
        Connection conn1 =
                DriverManager.getConnection("jdbc:sqlite:file:memdb1?mode=memory&cache=shared");
        Statement stmt1 = conn1.createStatement();
        stmt1.executeUpdate("create table tbl (col int)");
        stmt1.executeUpdate("insert into tbl values(100)");
        stmt1.close();

        Connection conn2 =
                DriverManager.getConnection("jdbc:sqlite:file:memdb1?mode=memory&cache=shared");
        Statement stmt2 = conn2.createStatement();
        ResultSet rs = stmt2.executeQuery("select * from tbl");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(100);
        stmt2.close();

        Connection conn3 = DriverManager.getConnection("jdbc:sqlite:file::memory:?cache=shared");
        Statement stmt3 = conn3.createStatement();
        stmt3.executeUpdate("attach 'file:memdb1?mode=memory&cache=shared' as memdb1");
        rs = stmt3.executeQuery("select * from memdb1.tbl");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(100);
        stmt3.executeUpdate("create table tbl2(col int)");
        stmt3.executeUpdate("insert into tbl2 values(200)");
        stmt3.close();

        Connection conn4 = DriverManager.getConnection("jdbc:sqlite:file::memory:?cache=shared");
        Statement stmt4 = conn4.createStatement();
        rs = stmt4.executeQuery("select * from tbl2");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(200);
        rs.close();
        stmt4.close();

        conn1.close();
        conn2.close();
        conn3.close();
        conn4.close();
    }

    @Test
    public void setPragmasFromURI() throws Exception {
        File testDB = copyToTemp("sample.db");

        assertThat(testDB.exists()).isTrue();
        Connection conn =
                DriverManager.getConnection(
                        String.format(
                                "jdbc:sqlite:%s?journal_mode=WAL&synchronous=OFF&journal_size_limit=500",
                                testDB));
        Statement stat = conn.createStatement();

        ResultSet rs = stat.executeQuery("pragma journal_mode");
        assertThat(rs.getString(1)).isEqualTo("wal");
        rs.close();

        rs = stat.executeQuery("pragma synchronous");
        assertThat(rs.getBoolean(1)).isEqualTo(false);
        rs.close();

        rs = stat.executeQuery("pragma journal_size_limit");
        assertThat(rs.getInt(1)).isEqualTo(500);
        rs.close();

        stat.close();
        conn.close();
    }

    @Test
    public void limits() throws Exception {
        File testDB = copyToTemp("sample.db");

        assertThat(testDB.exists()).isTrue();
        Connection conn =
                DriverManager.getConnection(
                        String.format("jdbc:sqlite:%s?limit_attached=0", testDB));
        Statement stat = conn.createStatement();

        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> stat.executeUpdate("ATTACH DATABASE attach_test.db AS attachDb"));

        stat.close();
        conn.close();
    }

    @Test
    public void ignoreUnknownParametersInURI() throws Exception {
        Connection conn =
                DriverManager.getConnection(
                        "jdbc:sqlite:file::memory:?cache=shared&foreign_keys=ON&debug=&invalid");
        Statement stat = conn.createStatement();

        ResultSet rs = stat.executeQuery("pragma foreign_keys");
        assertThat(rs.getBoolean(1)).isEqualTo(true);
        rs.close();

        stat.close();
        conn.close();
    }

    @Test
    public void errorOnEmptyPragmaValueInURI() {
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(
                        () ->
                                DriverManager.getConnection(
                                        "jdbc:sqlite:file::memory:?journal_mode=&synchronous="));
    }

    @Test
    public void ignoreDoubleAmpersandsInURI() throws Exception {
        File testDB = copyToTemp("sample.db");

        assertThat(testDB.exists()).isTrue();
        Connection conn =
                DriverManager.getConnection(
                        String.format(
                                "jdbc:sqlite:%s?synchronous=OFF&&&&journal_mode=WAL", testDB));
        Statement stat = conn.createStatement();

        ResultSet rs = stat.executeQuery("pragma journal_mode");
        assertThat(rs.getString(1)).isEqualTo("wal");
        rs.close();

        rs = stat.executeQuery("pragma synchronous");
        assertThat(rs.getBoolean(1)).isFalse();
        rs.close();

        stat.close();
        conn.close();
    }

    @Test
    public void useLastSpecifiedPragmaValueInURI() throws Exception {
        File testDB = copyToTemp("sample.db");

        assertThat(testDB.exists()).isTrue();
        Connection conn =
                DriverManager.getConnection(
                        String.format(
                                "jdbc:sqlite:%s?journal_mode=WAL&journal_mode=MEMORY&journal_mode=TRUNCATE",
                                testDB));
        Statement stat = conn.createStatement();

        ResultSet rs = stat.executeQuery("pragma journal_mode");
        assertThat(rs.getString(1)).isEqualTo("truncate");
        rs.close();

        stat.close();
        conn.close();
    }

    @Test
    public void overrideURIPragmaValuesWithProperties() throws Exception {
        File testDB = copyToTemp("sample.db");

        assertThat(testDB.exists()).isTrue();
        Properties props = new Properties();
        props.setProperty(Pragma.JOURNAL_MODE.pragmaName, JournalMode.TRUNCATE.name());
        Connection conn =
                DriverManager.getConnection(
                        String.format("jdbc:sqlite:%s?journal_mode=WAL", testDB), props);
        Statement stat = conn.createStatement();

        ResultSet rs = stat.executeQuery("pragma journal_mode");
        assertThat(rs.getString(1)).isEqualTo("truncate");
        rs.close();

        stat.close();
        conn.close();
    }
}
