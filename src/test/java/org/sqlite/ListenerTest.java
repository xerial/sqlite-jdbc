package org.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ListenerTest {

    private SQLiteConnection connectionOne, connectionTwo;

    @BeforeEach
    public void connect() throws Exception {
        File tmpFile = File.createTempFile("test-listeners", ".db");
        tmpFile.deleteOnExit();

        connectionOne =
                (SQLiteConnection)
                        DriverManager.getConnection("jdbc:sqlite:" + tmpFile.getAbsolutePath());
        connectionTwo =
                (SQLiteConnection)
                        DriverManager.getConnection("jdbc:sqlite:" + tmpFile.getAbsolutePath());

        Statement create = connectionOne.createStatement();
        create.execute(
                "CREATE TABLE IF NOT EXISTS sample (id INTEGER PRIMARY KEY AUTOINCREMENT, description TEXT);");
    }

    @Test
    public void testSetAndRemoveUpdateHook() throws Exception {
        final List<Update> updates = new LinkedList<Update>();

        SQLiteUpdateListener listener =
                new SQLiteUpdateListener() {
                    @Override
                    public void onUpdate(Type type, String database, String table, long rowId) {
                        synchronized (updates) {
                            updates.add(new Update(type, database, table, rowId));
                            updates.notifyAll();
                        }
                    }
                };

        connectionOne.addUpdateListener(listener);

        Statement statement = connectionOne.createStatement();
        statement.execute("INSERT INTO sample (description) VALUES ('smert za smert')");

        synchronized (updates) {
            if (updates.isEmpty()) {
                updates.wait(1000);
            }
        }

        if (updates.isEmpty()) throw new AssertionError("Never got update!");

        assertEquals(1, updates.size());
        assertEquals("sample", updates.get(0).table);
        assertEquals(1, updates.get(0).rowId);
        assertEquals(SQLiteUpdateListener.Type.INSERT, updates.get(0).type);

        updates.clear();

        connectionOne.removeUpdateListener(listener);

        Statement secondStatement = connectionOne.createStatement();
        secondStatement.execute("INSERT INTO sample (description) VALUES ('amor fati')");

        synchronized (updates) {
            if (updates.isEmpty()) {
                updates.wait(1000);
            }
        }

        assertTrue(updates.isEmpty());
    }

    @Test
    public void testMultiConnectionHook() throws Exception {
        final List<Update> updates = new LinkedList<Update>();

        SQLiteUpdateListener listener =
                new SQLiteUpdateListener() {
                    @Override
                    public void onUpdate(Type type, String database, String table, long rowId) {
                        synchronized (updates) {
                            updates.add(new Update(type, database, table, rowId));
                            updates.notifyAll();
                        }
                    }
                };

        connectionOne.addUpdateListener(listener);

        Statement statement = connectionOne.createStatement();
        statement.execute("INSERT INTO sample (description) VALUES ('smert za smert')");

        synchronized (updates) {
            if (updates.isEmpty()) {
                updates.wait(1000);
            }
        }

        if (updates.isEmpty()) throw new AssertionError("Never got update!");

        assertEquals(1, updates.size());
        assertEquals("sample", updates.get(0).table);
        assertEquals(1, updates.get(0).rowId);
        assertEquals(SQLiteUpdateListener.Type.INSERT, updates.get(0).type);

        updates.clear();

        Statement secondStatement = connectionTwo.createStatement();
        secondStatement.execute("INSERT INTO sample (description) VALUES ('amor fati')");

        synchronized (updates) {
            if (updates.isEmpty()) {
                updates.wait(1000);
            }
        }

        assertTrue(updates.isEmpty());

        connectionOne.removeUpdateListener(listener);
    }

    @Test
    public void testMultiInsertAndCommit() throws Exception {
        final List<Update> updates = new LinkedList<Update>();
        final AtomicBoolean committed = new AtomicBoolean(false);

        SQLiteUpdateListener updateListener =
                new SQLiteUpdateListener() {
                    @Override
                    public void onUpdate(Type type, String database, String table, long rowId) {
                        synchronized (updates) {
                            updates.add(new Update(type, database, table, rowId));
                            updates.notifyAll();
                        }
                    }
                };

        SQLiteCommitListener commitListener =
                new SQLiteCommitListener() {
                    @Override
                    public void onCommit() {
                        synchronized (committed) {
                            committed.set(true);
                        }
                    }

                    @Override
                    public void onRollback() {
                        throw new AssertionError("rollback?");
                    }
                };

        connectionOne.addUpdateListener(updateListener);
        connectionOne.addCommitListener(commitListener);

        connectionOne.setAutoCommit(false);

        for (int i = 0; i < 100; i++) {
            Statement statement = connectionOne.createStatement();
            statement.execute("INSERT INTO sample (description) VALUES ('test: " + i + "')");
        }

        connectionOne.setAutoCommit(true);

        synchronized (committed) {
            if (!committed.get()) {
                committed.wait(1000);
            }
        }

        assertTrue(committed.get());
        assertEquals(100, updates.size());

        for (int i = 0; i < 100; i++) {
            assertEquals(i + 1, updates.get(i).rowId);
            assertEquals("sample", updates.get(i).table);
            assertEquals(SQLiteUpdateListener.Type.INSERT, updates.get(i).type);
        }

        connectionOne.removeUpdateListener(updateListener);
        connectionOne.removeCommitListener(commitListener);
    }

    private static class Update {
        private final SQLiteUpdateListener.Type type;
        private final String database;
        private final String table;
        private final long rowId;

        private Update(SQLiteUpdateListener.Type type, String database, String table, long rowId) {
            this.type = type;
            this.database = database;
            this.table = table;
            this.rowId = rowId;
        }
    }
}
