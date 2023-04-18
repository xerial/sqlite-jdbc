package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.core.DB;
import org.sqlite.core.NativeDBHelper;

public class ListenerTest {

    private SQLiteConnection connectionOne, connectionTwo;

    @BeforeEach
    public void connect(@TempDir File tempDir) throws Exception {
        File tmpFile = File.createTempFile("test-listeners", ".db", tempDir);

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

    @AfterEach
    public void close() throws Exception {
        connectionOne.close();
        connectionTwo.close();
    }

    @Test
    public void testSetAndRemoveUpdateHook() throws Exception {
        final List<UpdateEvent> updates = new LinkedList<>();

        SQLiteUpdateListener listener =
                (type, database, table, rowId) -> {
                    synchronized (updates) {
                        updates.add(new UpdateEvent(type, database, table, rowId));
                        updates.notifyAll();
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

        assertThat(updates.size()).isEqualTo(1);
        assertThat(updates.get(0).table).isEqualTo("sample");
        assertThat(updates.get(0).rowId).isEqualTo(1);
        assertThat(updates.get(0).type).isEqualTo(SQLiteUpdateListener.Type.INSERT);

        updates.clear();

        connectionOne.removeUpdateListener(listener);

        Statement secondStatement = connectionOne.createStatement();
        secondStatement.execute("INSERT INTO sample (description) VALUES ('amor fati')");

        synchronized (updates) {
            if (updates.isEmpty()) {
                updates.wait(1000);
            }
        }

        assertThat(updates.isEmpty()).isTrue();
    }

    /**
     * Tests to ensure that the update/commit listeners work correctly when used on multiple
     * connections
     *
     * @throws Exception on test failure
     */
    @Test
    public void testMultiConnectionHook() throws Exception {
        CountingSQLiteUpdateListener listener1 = new CountingSQLiteUpdateListener();
        CountingSQLiteUpdateListener listener2 = new CountingSQLiteUpdateListener();
        CountingSQLiteCommitListener commitListener1 = new CountingSQLiteCommitListener();
        CountingSQLiteCommitListener commitListener2 = new CountingSQLiteCommitListener();

        connectionOne.addUpdateListener(listener1);
        connectionOne.addCommitListener(commitListener1);
        connectionTwo.addUpdateListener(listener2);
        connectionTwo.addCommitListener(commitListener2);

        try (Statement statement = connectionOne.createStatement()) {
            statement.execute("INSERT INTO sample (description) VALUES ('smert za smert')");
        }

        // update should be synchronous
        List<UpdateEvent> updates = listener1.getAllUpdates();

        if (updates.isEmpty()) throw new AssertionError("Never got update!");

        assertThat(updates.size()).isEqualTo(1);
        assertThat(updates.get(0).table).isEqualTo("sample");
        assertThat(updates.get(0).rowId).isEqualTo(1);
        assertThat(updates.get(0).type).isEqualTo(SQLiteUpdateListener.Type.INSERT);
        assertThat(commitListener1.getNumCommits()).isEqualTo(1);

        Statement secondStatement = connectionTwo.createStatement();
        secondStatement.execute("INSERT INTO sample (description) VALUES ('amor fati')");

        assertThat(listener1.getAllUpdates().isEmpty()).isTrue();
        assertThat(listener2.getAllUpdates().size()).isEqualTo(1);

        connectionOne.removeUpdateListener(listener1);
        connectionOne.removeCommitListener(commitListener2);
        connectionTwo.removeUpdateListener(listener2);
        connectionTwo.removeCommitListener(commitListener2);
    }

    /** A simple update listener that simply records the events that were updated. */
    static class CountingSQLiteUpdateListener implements SQLiteUpdateListener {
        final BlockingQueue<UpdateEvent> updates = new LinkedBlockingDeque<>();

        @Override
        public void onUpdate(Type type, String database, String table, long rowId) {
            if (!updates.offer(new UpdateEvent(type, database, table, rowId)))
                throw new RuntimeException("No space in queue");
        }

        /**
         * Get all the stored updates in the order they arrived in.
         *
         * @return the stored updates
         */
        public List<UpdateEvent> getAllUpdates() {
            List<UpdateEvent> ret = new ArrayList<>();
            updates.drainTo(ret);
            return ret;
        }
    }

    /**
     * Tests that listeners are called correctly when multiple inserts are made in a single commit.
     * The commit handler should be called a single time with auto commit off.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testMultiInsertAndCommit() throws Exception {

        CountingSQLiteUpdateListener updateListener = new CountingSQLiteUpdateListener();
        CountingSQLiteCommitListener commitListener = new CountingSQLiteCommitListener();

        connectionOne.addUpdateListener(updateListener);
        connectionOne.addCommitListener(commitListener);

        connectionOne.setAutoCommit(false);

        final int numStmts = 100;
        for (int i = 0; i < numStmts; i++) {
            try (Statement statement = connectionOne.createStatement()) {
                statement.execute("INSERT INTO sample (description) VALUES ('test: " + i + "')");
            }
        }

        connectionOne.setAutoCommit(true);

        // only one commit is done because we started with autocommit off..
        assertThat(commitListener.getNumCommits()).isEqualTo(1);
        List<UpdateEvent> updates = updateListener.getAllUpdates();
        assertThat(updates.size()).isEqualTo(numStmts);

        for (int i = 0; i < numStmts; i++) {
            assertThat(updates.get(i).rowId).isEqualTo(i + 1);
            assertThat(updates.get(i).table).isEqualTo("sample");
            assertThat(updates.get(i).type).isEqualTo(SQLiteUpdateListener.Type.INSERT);
        }

        connectionOne.removeUpdateListener(updateListener);
        connectionOne.removeCommitListener(commitListener);
    }

    /**
     * Tests to ensure that the update handler is cleaned up correctly when it is removed. This
     * ensures it should not leak native memory, and set any free'd pointers to null
     *
     * @throws Exception on test failure
     */
    @Test
    public void testUpdateHandlerCleanup() throws Exception {
        SQLiteConnection sqliteConnection = connectionOne;
        final DB database = sqliteConnection.getDatabase();

        CountingSQLiteUpdateListener updateListener = new CountingSQLiteUpdateListener();

        connectionOne.addUpdateListener(updateListener);
        assertThat(NativeDBHelper.getUpdateListener(database)).isNotEqualTo(0);
        connectionOne.removeUpdateListener(updateListener);
        assertThat(NativeDBHelper.getUpdateListener(database)).isEqualTo(0);

        connectionOne.addUpdateListener(updateListener);
        assertThat(NativeDBHelper.getUpdateListener(database)).isNotEqualTo(0);
        connectionOne.close();
        assertThat(NativeDBHelper.getUpdateListener(database)).isEqualTo(0);
    }

    /**
     * Tests to ensure that the commit handler is cleaned up correctly when it is removed. This
     * ensures it should not leak native memory, and set any free'd pointers to null
     *
     * @throws Exception on test failure
     */
    @Test
    public void testCommitHandlerCleanup() throws Exception {
        SQLiteConnection sqliteConnection = connectionOne;
        final DB database = sqliteConnection.getDatabase();

        CountingSQLiteCommitListener commitListener = new CountingSQLiteCommitListener();
        connectionOne.addCommitListener(commitListener);
        assertThat(NativeDBHelper.getCommitListener(database)).isNotEqualTo(0);
        connectionOne.removeCommitListener(commitListener);
        assertThat(NativeDBHelper.getCommitListener(database)).isEqualTo(0);

        connectionOne.addCommitListener(commitListener);
        assertThat(NativeDBHelper.getCommitListener(database)).isNotEqualTo(0);
        connectionOne.close();
        assertThat(NativeDBHelper.getCommitListener(database)).isEqualTo(0);
    }

    @Test
    public void testConnectionCloseWithAutoCommitDisabledAndCommitListener() throws Exception {
        CountingSQLiteCommitListener commitListener = new CountingSQLiteCommitListener();
        connectionOne.setAutoCommit(false);
        connectionOne.addCommitListener(commitListener);
        connectionOne.close();
    }

    /** A helper class that simply counts the number of commits operations that were done. */
    static class CountingSQLiteCommitListener implements SQLiteCommitListener {
        final AtomicInteger committed = new AtomicInteger(0);

        @Override
        public void onCommit() {
            committed.incrementAndGet();
        }

        @Override
        public void onRollback() {
            throw new AssertionError("rollback?");
        }

        public int getNumCommits() {
            return committed.get();
        }
    }

    /**
     * A helper class that stores information about an update event, to validate that the update
     * callbacks work as expected
     */
    private static class UpdateEvent {
        private final SQLiteUpdateListener.Type type;
        private final String database;
        private final String table;
        private final long rowId;

        private UpdateEvent(
                SQLiteUpdateListener.Type type, String database, String table, long rowId) {
            this.type = type;
            this.database = database;
            this.table = table;
            this.rowId = rowId;
        }
    }
}
