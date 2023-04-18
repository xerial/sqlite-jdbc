// --------------------------------------
// sqlite-jdbc Project
//
// JDBCTest.java
// Since: Apr 8, 2009
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JDBCTest {
    @Test
    public void enableLoadExtensionTest() throws Exception {
        Properties prop = new Properties();
        prop.setProperty("enable_load_extension", "true");

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:", prop)) {
            Statement stat = conn.createStatement();

            // How to build shared lib in Windows
            // # mingw32-gcc -fPIC -c extension-function.c
            // # mingw32-gcc -shared -Wl -o extension-function.dll extension-function.o

            //            stat.executeQuery("select load_extension('extension-function.dll')");
            //
            //            ResultSet rs = stat.executeQuery("select sqrt(4)");
            //            System.out.println(rs.getDouble(1));

        }
    }

    @Test
    public void majorVersion() throws Exception {
        int major = DriverManager.getDriver("jdbc:sqlite:").getMajorVersion();
        int minor = DriverManager.getDriver("jdbc:sqlite:").getMinorVersion();
    }

    @Test
    public void shouldReturnNullIfProtocolUnhandled() throws Exception {
        assertThat(JDBC.createConnection("jdbc:anotherpopulardatabaseprotocol:", null)).isNull();
    }

    @Test
    public void allDriverPropertyInfoShouldHaveADescription() throws Exception {
        Driver driver = DriverManager.getDriver("jdbc:sqlite:");
        assertThat(driver.getPropertyInfo(null, null))
                .allSatisfy((info) -> assertThat(info.description).isNotNull());
    }

    @Test
    public void pragmaReadOnly() throws SQLException {
        SQLiteConnection connection =
                (SQLiteConnection)
                        DriverManager.getConnection(
                                "jdbc:sqlite::memory:?jdbc.explicit_readonly=true");
        assertThat(connection.getDatabase().getConfig().isExplicitReadOnly()).isTrue();
    }

    @Test
    public void canSetJdbcConnectionToReadOnly() throws Exception {
        SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            assertThat(connection.isReadOnly()).isFalse();
            connection.setReadOnly(true);
            assertThat(connection.isReadOnly()).isTrue();
            connection.setReadOnly(false);
            assertThat(connection.isReadOnly()).isFalse();
            connection.setReadOnly(true);
            assertThat(connection.isReadOnly()).isTrue();
        }
    }

    @Test
    public void cannotSetJdbcConnectionToReadOnlyAfterFirstStatement() throws Exception {
        SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            // execute a statement
            try (Statement statement = connection.createStatement()) {
                boolean success = statement.execute("SELECT * FROM sqlite_schema");
                assertThat(success).isTrue();
            }
            // try to assign read-only
            assertThatExceptionOfType(SQLException.class)
                    .as("Managed to set readOnly = true on a dirty connection!")
                    .isThrownBy(() -> connection.setReadOnly(true));
        }
    }

    @Test
    public void canSetJdbcConnectionToReadOnlyAfterCommit() throws Exception {
        SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.setReadOnly(true);
            // execute a statement
            try (Statement statement = connection.createStatement()) {
                boolean success = statement.execute("SELECT * FROM sqlite_schema");
                assertThat(success).isTrue();
            }
            connection.commit();

            // try to assign a new read-only value
            connection.setReadOnly(false);
        }
    }

    @Test
    public void canSetJdbcConnectionToReadOnlyAfterRollback() throws Exception {
        System.out.println("Creating JDBC Datasource");
        SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        System.out.println("Creating JDBC Connection");
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("JDBC Connection created");
            System.out.println("Disabling auto-commit");
            connection.setAutoCommit(false);
            System.out.println("Creating statement");
            // execute a statement
            try (Statement statement = connection.createStatement()) {
                System.out.println("Executing query");
                boolean success = statement.execute("SELECT * FROM sqlite_schema");
                assertThat(success).isTrue();
            } finally {
                System.out.println("Closing statement");
            }
            System.out.println("Performing rollback");
            connection.rollback();

            System.out.println("Setting connection to read-only");
            // try to assign read-only
            connection.setReadOnly(true);
            // execute a statement
            try (Statement statement2 = connection.createStatement()) {
                System.out.println("Executing query 2");
                boolean success = statement2.execute("SELECT * FROM sqlite_schema");
                assertThat(success).isTrue();
            } finally {
                System.out.println("Closing statement 2");
            }
            System.out.println("Performing rollback 2");
            connection.rollback();
        }
    }

    @Test
    public void cannotExecuteUpdatesWhenConnectionIsSetToReadOnly() throws Exception {
        SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.setReadOnly(true);

            // execute a statement
            try (Statement statement = connection.createStatement()) {
                assertThatExceptionOfType(SQLException.class)
                        .as("Managed to modify DB contents on a read-only connection!")
                        .isThrownBy(
                                () ->
                                        statement.execute(
                                                "CREATE TABLE TestTable(ID VARCHAR(255), PRIMARY KEY(ID))"));
            }
            connection.rollback();

            // try to assign read-only
            connection.setReadOnly(true);
        }
    }

    @Test
    void name() {}

    @Test
    public void jdbcHammer(@TempDir File tempDir) throws Exception {
        final SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        File tempFile = File.createTempFile("myTestDB", ".db", tempDir);
        dataSource.setUrl("jdbc:sqlite:" + tempFile.getAbsolutePath());
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE TestTable(ID INT, testval INT, PRIMARY KEY(ID));");
                stmt.executeUpdate("INSERT INTO TestTable (ID, testval) VALUES(1, 0);");
            }
            connection.commit();
        }

        final AtomicInteger count = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Thread thread =
                    new Thread(
                            () -> {
                                for (int i1 = 0; i1 < 100; i1++) {
                                    try {
                                        try (Connection connection = dataSource.getConnection()) {
                                            connection.setAutoCommit(false);
                                            boolean read = Math.random() < 0.5;
                                            if (read) {
                                                connection.setReadOnly(true);
                                                try (Statement statement =
                                                        connection.createStatement()) {
                                                    ResultSet rs =
                                                            statement.executeQuery(
                                                                    "SELECT * FROM TestTable");
                                                    rs.close();
                                                }
                                            } else {
                                                try (Statement statement =
                                                        connection.createStatement()) {
                                                    try (ResultSet rs =
                                                            statement.executeQuery(
                                                                    "SELECT * FROM TestTable")) {
                                                        while (rs.next()) {
                                                            int id = rs.getInt("ID");
                                                            int value = rs.getInt("testval");
                                                            count.incrementAndGet();
                                                            statement.executeUpdate(
                                                                    "UPDATE TestTable SET testval = "
                                                                            + (value + 1)
                                                                            + " WHERE ID = "
                                                                            + id);
                                                        }
                                                    }
                                                }
                                                connection.commit();
                                            }
                                        }
                                    } catch (SQLException e) {
                                        throw new RuntimeException("Worker failed", e);
                                    }
                                }
                            });
            thread.setName("Worker #" + (i + 1));
            threads.add(thread);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        try (Connection connection2 = dataSource.getConnection()) {
            connection2.setAutoCommit(false);
            connection2.setReadOnly(true);
            try (Statement stmt = connection2.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM TestTable")) {
                    assertThat(rs.next()).isTrue();
                    int id = rs.getInt("ID");
                    int val = rs.getInt("testval");
                    assertThat(id).isEqualTo(1);
                    assertThat(val).isEqualTo(count.get());
                    assertThat(rs.next()).isFalse();
                }
            }
            connection2.commit();
        }
    }

    // helper methods -----------------------------------------------------------------

    private SQLiteDataSource createDatasourceWithExplicitReadonly() {
        //        DriverManager.setLogWriter(new PrintWriter(System.out));
        SQLiteConfig config = new SQLiteConfig();
        config.setExplicitReadOnly(true);
        config.setBusyTimeout(10000);

        return new SQLiteDataSource(config);
    }
}
