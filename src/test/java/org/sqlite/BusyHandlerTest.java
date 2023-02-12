package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.core.DB;
import org.sqlite.core.NativeDBHelper;

public class BusyHandlerTest {
    private Connection conn;
    private Statement stat;
    @TempDir Path tempDir;

    @BeforeEach
    public void connect() throws Exception {
        conn = createConnection(0);
        stat = conn.createStatement();
    }

    /**
     * Create a unique db for the specified thread number
     *
     * @param threadNum the thread number
     * @return the connection
     * @throws SQLException if the connection cannot be established
     */
    private Connection createConnection(int threadNum) throws SQLException {
        return DriverManager.getConnection(
                "jdbc:sqlite:" + tempDir.resolve("test" + threadNum + ".db"));
    }

    @AfterEach
    public void close() throws SQLException {
        stat.close();
        conn.close();
    }

    /** An internal helper class which tests that BusyHandlers are thread safe */
    public class BusyWork extends Thread {
        private final Connection busyWorkConn;
        private final Statement stat;
        private final CountDownLatch lockedLatch = new CountDownLatch(1);
        private final CountDownLatch completeLatch = new CountDownLatch(1);

        public BusyWork(int threadNum) throws Exception {
            busyWorkConn = createConnection(threadNum);
            Function.create(
                    busyWorkConn,
                    "wait_for_latch",
                    new Function() {
                        @Override
                        protected void xFunc() throws SQLException {
                            lockedLatch.countDown();
                            try {
                                completeLatch.await();
                            } catch (InterruptedException e) {
                                throw new SQLException("Interrupted");
                            }
                            result(100);
                        }
                    });
            stat = busyWorkConn.createStatement();
            stat.setQueryTimeout(1);
        }

        @Override
        public void run() {
            try {
                // Generate some work for the sqlite vm
                stat.executeUpdate("drop table if exists foo;");
                stat.executeUpdate("create table foo (id integer);");
                stat.execute("insert into foo (id) values (wait_for_latch());");
            } catch (SQLException ex) {
                System.out.println("HERE" + ex);
                throw new RuntimeException(ex);
            } finally {
                try {
                    busyWorkConn.close();
                } catch (Exception ex) {
                    System.out.println("Exception closing: " + ex);
                    ex.printStackTrace();
                }
            }
        }
    }

    private static void doWork(Statement stat) throws SQLException {
        // Generate some work for the sqlite vm
        int i = 0;
        while (i < 5) {
            stat.execute("insert into foo (id) values (" + i + ")");
            i++;
        }
    }

    /**
     * A basic test to make sure that busy callback handlers are processed as expected
     *
     * @throws Exception on test failure
     */
    @Test
    @Disabled("This test is very flaky; disabling it for now")
    public void basicBusyHandler() throws Exception {
        basicBusyHandler(0);
    }

    private void basicBusyHandler(int threadNum) throws Exception {
        try (Connection localConn = createConnection(threadNum)) {
            final int[] calls = {0};
            BusyHandler.setHandler(
                    localConn,
                    new BusyHandler() {
                        @Override
                        protected int callback(int nbPrevInvok) {
                            assertThat(calls[0]).isEqualTo(nbPrevInvok);
                            calls[0]++;

                            if (nbPrevInvok <= 1) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                    });

            BusyWork busyWork = new BusyWork(threadNum);
            busyWork.start();

            // let busyWork block inside insert
            busyWork.lockedLatch.await();

            try (Statement localStat = localConn.createStatement()) {
                Throwable thrown = catchThrowable(() -> doWork(localStat));
                assertThat(thrown).isInstanceOf(SQLiteException.class);
                assertThat(((SQLiteException) thrown).getErrorCode())
                        .isEqualTo(SQLiteErrorCode.SQLITE_BUSY.code);
            }

            busyWork.completeLatch.countDown();
            busyWork.join();
            assertThat(calls[0]).isEqualTo(3);
        }
    }

    /**
     * Tests that unregistering a busy handler works as expected
     *
     * @throws Exception on test failure
     */
    @Test
    @Disabled("This test is very flaky; disabling it for now")
    public void testUnregister() throws Exception {
        final int[] calls = {0};
        BusyHandler.setHandler(
                conn,
                new BusyHandler() {
                    @Override
                    protected int callback(int nbPrevInvok) {
                        assertThat(calls[0]).isEqualTo(nbPrevInvok);
                        calls[0]++;

                        if (nbPrevInvok <= 1) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });

        BusyWork busyWork = new BusyWork(0);
        busyWork.start();
        // let busyWork block inside insert
        busyWork.lockedLatch.await();
        Throwable thrown = catchThrowable(() -> doWork(stat));
        assertThat(thrown).isInstanceOf(SQLiteException.class);
        assertThat(((SQLiteException) thrown).getErrorCode())
                .isEqualTo(SQLiteErrorCode.SQLITE_BUSY.code);
        busyWork.completeLatch.countDown();
        busyWork.join();
        assertThat(calls[0]).isEqualTo(3);

        int totalCalls = calls[0];
        BusyHandler.clearHandler(conn);
        busyWork = new BusyWork(0);
        busyWork.start();
        // let busyWork block inside insert
        busyWork.lockedLatch.await();
        thrown = catchThrowable(() -> doWork(stat));
        assertThat(thrown).isInstanceOf(SQLiteException.class);
        assertThat(((SQLiteException) thrown).getErrorCode())
                .isEqualTo(SQLiteErrorCode.SQLITE_BUSY.code);

        busyWork.completeLatch.countDown();
        busyWork.join();
        assertThat(calls[0]).isEqualTo(totalCalls);
    }

    /**
     * Tests to make sure that clearing the busy handler works as expected, and does not double
     * free, etc.
     */
    @Test
    public void testRemovingBusyHandler() throws Exception {

        SQLiteConnection sqliteConnection = (SQLiteConnection) conn;
        setDummyHandler();
        final DB database = sqliteConnection.getDatabase();
        assertThat(NativeDBHelper.getBusyHandler(database)).isNotEqualTo(0);
        BusyHandler.clearHandler(conn);
        assertThat(NativeDBHelper.getBusyHandler(database)).isEqualTo(0);
        BusyHandler.clearHandler(conn);

        setDummyHandler();
        assertThat(NativeDBHelper.getBusyHandler(database)).isNotEqualTo(0);
        BusyHandler.setHandler(conn, null);
        assertThat(NativeDBHelper.getBusyHandler(database)).isEqualTo(0);
        BusyHandler.setHandler(conn, null);

        setDummyHandler();
        assertThat(NativeDBHelper.getBusyHandler(database)).isNotEqualTo(0);
        conn.close();
        assertThat(NativeDBHelper.getBusyHandler(database)).isEqualTo(0);
    }

    private void setDummyHandler() throws SQLException {
        BusyHandler.setHandler(
                conn,
                new BusyHandler() {
                    @Override
                    protected int callback(int nbPrevInvok) {
                        return 0;
                    }
                });
    }

    /**
     * Tests that adding busy handlers to different connections in multiple threads works as
     * expected. This test finds obvious race conditions such as a busy handler being set for the
     * application state globally rather than per connection.
     */
    @Test
    public void testMultiThreaded() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int threadNum = 0; threadNum < 4; threadNum++) {
            final int runnerNum = threadNum; // lambdas cannot take mutable ints
            futures.add(
                    CompletableFuture.runAsync(
                            () -> {
                                try {
                                    for (int i = 0; i < 10; ++i) {
                                        basicBusyHandler(runnerNum);
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }));
        }

        // if any of these threads fail, we'll get an exception
        for (CompletableFuture<?> fut : futures) fut.join();
    }
}
