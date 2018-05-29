package org.sqlite;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
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

        public BusyWork() throws Exception {
            conn = DriverManager.getConnection("jdbc:sqlite:target/test.db");
            stat = conn.createStatement();
            stat.setQueryTimeout(1);
        }

        @Override
        public void run(){

            try {
                synchronized(this) {
                    // Generate some work for the sqlite vm
                    stat.executeUpdate("drop table if exists foo;");
                    stat.executeUpdate("create table foo (id integer);");

                    int i = 0;
                    while (i<10000) {
                        String stmt = "insert into foo (id) values (" + i + ")";
                        stat.addBatch(stmt);
                        i++;
                    }
                }
            } catch (SQLException ex) {System.out.println("HERE"+ex.toString());}

            try {
                stat.executeBatch();
            } catch (SQLException ex) {System.out.println("BLOB"+ex.toString());}
        }
    }

    private void workWork() throws SQLException, InterruptedException {
        // I let busyWork inject first so it can busy the db
        Thread.sleep(1000);

        // Generate some work for the sqlite vm
        int i = 0;
        while (i<5) {
            stat.executeQuery("insert into foo (id) values (" + i + ")");
            i++;
        }
    }

    @Test
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

        // I let busyWork prepare a huge insert
        Thread.sleep(1000);

        synchronized(busyWork){
            try{
                workWork();
            } catch(SQLException ex) {
                assertEquals(SQLiteErrorCode.SQLITE_BUSY.code, ex.getErrorCode());
            }
        }

        busyWork.interrupt();
        assertEquals(3, calls[0]);
    }

    @Test
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
        // I let busyWork prepare a huge insert
        Thread.sleep(1000);
        synchronized(busyWork){
            try{
                workWork();
            } catch(SQLException ex) {
                assertEquals(SQLiteErrorCode.SQLITE_BUSY.code, ex.getErrorCode());
            }
        }
        assertEquals(3, calls[0]);

        int totalCalls = calls[0];
        BusyHandler.clearHandler(conn);
        busyWork = new BusyWork();
        busyWork.start();
        // I let busyWork prepare a huge insert
        Thread.sleep(1000);
        synchronized(busyWork){
            try{
                workWork();
            } catch(SQLException ex) {
                assertEquals(SQLiteErrorCode.SQLITE_BUSY.code, ex.getErrorCode());
            }
        }

        busyWork.interrupt();
        assertEquals(totalCalls, calls[0]);
    }

    @Test
    public void testInterrupt() throws Exception {

        try {
            BusyHandler.setHandler(conn, new BusyHandler() {
                @Override
                protected int callback(int nbPrevInvok) throws SQLException {
                    return 1;
                }
            });
            workWork();
        } catch (SQLException ex) {
            // Expected error
            return;
        }
        // Progress function throws, not reached
        fail();
    }
}
