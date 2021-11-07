package org.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests to ensure that incorrect usage of prepared statements does not result in a java
 * segmentation fault
 */
public class PreparedStatementThreadTest {
    private Connection conn;
    private Statement stat;

    @BeforeEach
    public void connect() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
    }

    @AfterEach
    public void close() throws SQLException {
        stat.close();
        conn.close();
    }

    /**
     * Tests to make sure that if one thread is uses a PreparedStatement which another thread is
     * closing, that the application throws an exception rather than crashing due to undefined C
     * behavior
     */
    @Test
    public void multipleThreadCloseSegmentationFault() throws SQLException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 100; i++) {
                testRace(executorService);
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    private void testRace(ExecutorService executorService) throws SQLException {
        AtomicInteger countdown = new AtomicInteger();
        PreparedStatement prep = conn.prepareStatement("select 1,2,3,4,5");

        Future<Boolean> queryThread =
                executorService.submit(
                        () -> {
                            waitFor(countdown);
                            for (int i = 0; i < 100; i++) {
                                ResultSet set = prep.executeQuery();
                                assertTrue(set.next());
                                for (int j = 1; j <= 5; j++) assertEquals(j, set.getInt(j));
                            }
                            return true;
                        });
        Future<Boolean> closeThread =
                executorService.submit(
                        () -> {
                            waitFor(countdown);
                            prep.close();
                            return true;
                        });
        try {
            assertTrue(queryThread.get());
            assertTrue(closeThread.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        } catch (ExecutionException e) {
            // this is OK, so long as we don't seg fault
        }
    }

    private void waitFor(AtomicInteger countdown) {
        countdown.decrementAndGet();
        while (0 < countdown.get())
            ;
    }
}
