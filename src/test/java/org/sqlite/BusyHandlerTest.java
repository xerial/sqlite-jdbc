package org.sqlite;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.fail;

public class BusyHandlerTest {
    private Connection conn;
    private Statement stat;

    @Before
    public void connect() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:target/test.db");
        stat = conn.createStatement();
    }

    @After
    public void close() throws SQLException {
        stat.close();
        conn.close();
    }

    public class BusyWork extends Thread {
        private final Connection conn;
        private final Statement stat;
        private final CountDownLatch lockedLatch = new CountDownLatch(1);
        private final CountDownLatch completeLatch = new CountDownLatch(1);

        public BusyWork() throws Exception {
            conn = DriverManager.getConnection("jdbc:sqlite:target/test.db");
            Function.create(conn, "wait_for_latch", new Function() {
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
            stat = conn.createStatement();
            stat.setQueryTimeout(1);
        }

        @Override
        public void run(){
            try {
                // Generate some work for the sqlite vm
                stat.executeUpdate("drop table if exists foo;");
                stat.executeUpdate("create table foo (id integer);");
                stat.execute("insert into foo (id) values (wait_for_latch());");
            } catch (SQLException ex) {System.out.println("HERE"+ex.toString());}
        }
    }

    private void workWork() throws SQLException {
        // Generate some work for the sqlite vm
        int i = 0;
        while (i<5) {
            stat.execute("insert into foo (id) values (" + i + ")");
            i++;
        }
    }

    @Test
    @Ignore("This test is very flaky; disabling it for now")
    public void basicBusyHandler() throws Exception {
        final int[] calls = {0};
        BusyHandler.setHandler(conn, new BusyHandler() {
            @Override
            protected int callback(int nbPrevInvok) throws SQLException {
                assertEquals(nbPrevInvok, calls[0]);
                calls[0]++;

                if (nbPrevInvok <= 1) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        BusyWork busyWork = new BusyWork();
        busyWork.start();

        // let busyWork block inside insert
        busyWork.lockedLatch.await();

        try{
            workWork();
            fail("Should throw SQLITE_BUSY exception");
        } catch(SQLException ex) {
            assertEquals(SQLiteErrorCode.SQLITE_BUSY.code, ex.getErrorCode());
        }

        busyWork.completeLatch.countDown();
        busyWork.join();
        assertEquals(3, calls[0]);
    }

    @Test
    @Ignore("This test is very flaky; disabling it for now")
    public void testUnregister() throws Exception {
        final int[] calls = {0};
        BusyHandler.setHandler(conn, new BusyHandler() {
            @Override
            protected int callback(int nbPrevInvok) throws SQLException {
                assertEquals(nbPrevInvok, calls[0]);
                calls[0]++;

                if (nbPrevInvok <= 1) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        BusyWork busyWork = new BusyWork();
        busyWork.start();
        // let busyWork block inside insert
        busyWork.lockedLatch.await();
        try{
            workWork();
            fail("Should throw SQLITE_BUSY exception");
        } catch(SQLException ex) {
            assertEquals(SQLiteErrorCode.SQLITE_BUSY.code, ex.getErrorCode());
        }
        busyWork.completeLatch.countDown();
        busyWork.join();
        assertEquals(3, calls[0]);

        int totalCalls = calls[0];
        BusyHandler.clearHandler(conn);
        busyWork = new BusyWork();
        busyWork.start();
        // let busyWork block inside insert
        busyWork.lockedLatch.await();
        try{
            workWork();
            fail("Should throw SQLITE_BUSY exception");
        } catch(SQLException ex) {
            assertEquals(SQLiteErrorCode.SQLITE_BUSY.code, ex.getErrorCode());
        }

        busyWork.completeLatch.countDown();
        busyWork.join();
        assertEquals(totalCalls, calls[0]);
    }
}
