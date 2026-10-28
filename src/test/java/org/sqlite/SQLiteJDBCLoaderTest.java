/*--------------------------------------------------------------------------
 *  Copyright 2007 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
// --------------------------------------
// sqlite-jdbc Project
//
// SQLiteJDBCLoaderTest.java
// Since: Oct 15, 2007
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

public class SQLiteJDBCLoaderTest {

    private Connection connection = null;

    @BeforeEach
    public void setUp() throws Exception {
        connection = null;
        // create a database connection
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void query() {
        // if e.getMessage() is "out of memory", it probably means no
        // database file is found
        assertThatNoException()
                .isThrownBy(
                        () -> {
                            Statement statement = connection.createStatement();
                            statement.setQueryTimeout(30); // set timeout to 30 sec.

                            statement.executeUpdate(
                                    "create table person ( id integer, name string)");
                            statement.executeUpdate("insert into person values(1, 'leo')");
                            statement.executeUpdate("insert into person values(2, 'yui')");

                            ResultSet rs =
                                    statement.executeQuery("select * from person order by id");
                            while (rs.next()) {
                                // read the result set
                                rs.getInt(1);
                                rs.getString(2);
                            }
                        });
    }

    @Test
    public void function() throws SQLException {
        Function.create(
                connection,
                "total",
                new Function() {
                    @Override
                    protected void xFunc() throws SQLException {
                        int sum = 0;
                        for (int i = 0; i < args(); i++) {
                            sum += value_int(i);
                        }
                        result(sum);
                    }
                });

        ResultSet rs = connection.createStatement().executeQuery("select total(1, 2, 3, 4, 5)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(1 + 2 + 3 + 4 + 5);
    }

    @Test
    public void version() {
        assertThat(SQLiteJDBCLoader.getVersion()).isNotEqualTo("unknown");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS) // POSIX permissions
    public void extractedLibraryIsPrivateToTheCurrentUser() throws Exception {
        assertThat(SQLiteJDBCLoader.initialize()).isTrue();

        // the library extracted by this JVM: same version prefix, not a lock file, and created
        // after the JVM started (older files belong to other JVMs)
        Path tempDir =
                Paths.get(
                        System.getProperty(
                                "org.sqlite.tmpdir", System.getProperty("java.io.tmpdir")));
        String prefix = "sqlite-" + SQLiteJDBCLoader.getVersion() + "-";
        long jvmStart = ManagementFactory.getRuntimeMXBean().getStartTime() - 1000;
        List<Path> extracted;
        try (Stream<Path> files = Files.list(tempDir)) {
            extracted =
                    files.filter(p -> p.getFileName().toString().startsWith(prefix))
                            .filter(p -> !p.getFileName().toString().endsWith(".lck"))
                            .filter(p -> lastModified(p) >= jvmStart)
                            .collect(Collectors.toList());
        }
        assumeTrue(!extracted.isEmpty(), "native library was not extracted by this JVM");

        // the temp folder is shared with other users, the extracted library is the code loaded
        // into the JVM and must not be readable or writable by anyone else
        for (Path lib : extracted) {
            assertThat(Files.getPosixFilePermissions(lib))
                    .as(lib.toString())
                    .isSubsetOf(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE);
        }
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    @Test
    public void test(@TempDir Path tmpDir) throws Throwable {
        final AtomicInteger completedThreads = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(32);
        for (int i = 0; i < 32; i++) {
            final String connStr = "jdbc:sqlite:" + tmpDir.resolve("sample-" + i + ".db");
            final int sleepMillis = i;
            pool.execute(
                    () -> {
                        try {
                            Thread.sleep(sleepMillis * 10);
                        } catch (InterruptedException ignored) {
                        }
                        assertThatNoException()
                                .isThrownBy(
                                        () -> {
                                            // Uncomment the synchronized block and everything
                                            // works.
                                            // synchronized (TestSqlite.class) {
                                            Connection conn = DriverManager.getConnection(connStr);
                                            conn.close();
                                            // }
                                        });
                        completedThreads.incrementAndGet();
                    });
        }
        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.SECONDS);
        assertThat(completedThreads.get()).isEqualTo(32);
    }
}
