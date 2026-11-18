package org.sqlite;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Compile-time authorization callback, see <a
 * href="https://www.sqlite.org/c3ref/set_authorizer.html">sqlite3_set_authorizer</a>.
 *
 * <p>SQLite invokes {@link #authorize} while a statement is being <b>prepared</b> (and when SQLite
 * transparently re-prepares it, e.g. after a schema change). Every table/column read or write is
 * checked, including accesses made indirectly through views, triggers and subqueries; in that case
 * {@code triggerOrView} names the innermost trigger or view responsible for the access.
 *
 * <h3>Lifecycle</h3>
 *
 * <ul>
 *   <li>An authorizer is scoped to a single connection. At most one authorizer is registered per
 *       connection; {@link #setAuthorizer} replaces any previously registered one.
 *   <li>It is removed with {@link #clearAuthorizer} (or {@code setAuthorizer(conn, null)}), and
 *       automatically when the connection is closed.
 *   <li>Setting or clearing an authorizer expires all existing prepared statements of the
 *       connection: they are re-prepared, and therefore re-checked against the new authorizer, the
 *       next time they are executed. A statement that is already running (e.g. an open {@link
 *       java.sql.ResultSet}) finishes under the policy it was prepared with.
 *   <li>It also sees statements issued internally by the driver (e.g. {@code BEGIN}, {@code
 *       COMMIT}, {@code PRAGMA} used by metadata queries), so return {@link #SQLITE_OK} for actions
 *       you do not care about.
 *   <li>The driver's extended {@code BACKUP} and {@code RESTORE} commands (see {@link
 *       ExtendedCommand}) use the SQLite backup API instead of preparing SQL, so they are
 *       <b>not</b> checked by the authorizer. If untrusted SQL is executed, it is up to the
 *       application to filter these commands.
 * </ul>
 *
 * <h3>Threading</h3>
 *
 * The callback runs synchronously on the thread that prepares the statement on this connection.
 * Since the driver serializes all native calls of a connection, the callback is never invoked
 * concurrently for the same connection; it is thread-confined to the connection for the duration of
 * each call. The same instance must not be shared between connections unless it is thread-safe.
 * The callback must not use the connection it is registered on (SQLite forbids this).
 *
 * <h3>Return values and errors</h3>
 *
 * <ul>
 *   <li>{@link #SQLITE_OK}: allow the action.
 *   <li>{@link #SQLITE_DENY}: abort preparing the statement; it fails with an {@link SQLException}
 *       whose error code is {@link SQLiteErrorCode#SQLITE_AUTH}.
 *   <li>{@link #SQLITE_IGNORE}: for {@link #SQLITE_READ} the column reads as NULL; for other
 *       actions see the SQLite documentation.
 *   <li>Any other value makes preparation fail with an {@code SQLITE_ERROR} ("authorizer
 *       malfunction").
 *   <li>If the callback throws, the exception is discarded and the action is <b>denied</b> (fail
 *       closed), i.e. the statement fails with {@link SQLiteErrorCode#SQLITE_AUTH}.
 * </ul>
 */
public abstract class Authorizer {

    /** Allow the action. */
    public static final int SQLITE_OK = 0;

    /** Reject the statement with {@link SQLiteErrorCode#SQLITE_AUTH}. */
    public static final int SQLITE_DENY = 1;

    /** Disallow the action without failing the statement (e.g. a read returns NULL). */
    public static final int SQLITE_IGNORE = 2;

    // Action codes. The meaning of arg1/arg2 passed to authorize() depends on the action.

    /** arg1: index name, arg2: table name. */
    public static final int SQLITE_CREATE_INDEX = 1;

    /** arg1: table name, arg2: null. */
    public static final int SQLITE_CREATE_TABLE = 2;

    /** arg1: index name, arg2: table name. */
    public static final int SQLITE_CREATE_TEMP_INDEX = 3;

    /** arg1: table name, arg2: null. */
    public static final int SQLITE_CREATE_TEMP_TABLE = 4;

    /** arg1: trigger name, arg2: table name. */
    public static final int SQLITE_CREATE_TEMP_TRIGGER = 5;

    /** arg1: view name, arg2: null. */
    public static final int SQLITE_CREATE_TEMP_VIEW = 6;

    /** arg1: trigger name, arg2: table name. */
    public static final int SQLITE_CREATE_TRIGGER = 7;

    /** arg1: view name, arg2: null. */
    public static final int SQLITE_CREATE_VIEW = 8;

    /** arg1: table name, arg2: null. */
    public static final int SQLITE_DELETE = 9;

    /** arg1: index name, arg2: table name. */
    public static final int SQLITE_DROP_INDEX = 10;

    /** arg1: table name, arg2: null. */
    public static final int SQLITE_DROP_TABLE = 11;

    /** arg1: index name, arg2: table name. */
    public static final int SQLITE_DROP_TEMP_INDEX = 12;

    /** arg1: table name, arg2: null. */
    public static final int SQLITE_DROP_TEMP_TABLE = 13;

    /** arg1: trigger name, arg2: table name. */
    public static final int SQLITE_DROP_TEMP_TRIGGER = 14;

    /** arg1: view name, arg2: null. */
    public static final int SQLITE_DROP_TEMP_VIEW = 15;

    /** arg1: trigger name, arg2: table name. */
    public static final int SQLITE_DROP_TRIGGER = 16;

    /** arg1: view name, arg2: null. */
    public static final int SQLITE_DROP_VIEW = 17;

    /** arg1: table name, arg2: null. */
    public static final int SQLITE_INSERT = 18;

    /** arg1: pragma name, arg2: pragma argument or null. */
    public static final int SQLITE_PRAGMA = 19;

    /** arg1: table name, arg2: column name. */
    public static final int SQLITE_READ = 20;

    /** arg1: null, arg2: null. */
    public static final int SQLITE_SELECT = 21;

    /** arg1: operation, arg2: null. */
    public static final int SQLITE_TRANSACTION = 22;

    /** arg1: table name, arg2: column name. */
    public static final int SQLITE_UPDATE = 23;

    /** arg1: filename, arg2: null. */
    public static final int SQLITE_ATTACH = 24;

    /** arg1: database name, arg2: null. */
    public static final int SQLITE_DETACH = 25;

    /** arg1: database name, arg2: table name. */
    public static final int SQLITE_ALTER_TABLE = 26;

    /** arg1: index name, arg2: null. */
    public static final int SQLITE_REINDEX = 27;

    /** arg1: table name, arg2: null. */
    public static final int SQLITE_ANALYZE = 28;

    /** arg1: table name, arg2: module name. */
    public static final int SQLITE_CREATE_VTABLE = 29;

    /** arg1: table name, arg2: module name. */
    public static final int SQLITE_DROP_VTABLE = 30;

    /** arg1: null, arg2: function name. */
    public static final int SQLITE_FUNCTION = 31;

    /** arg1: operation, arg2: savepoint name. */
    public static final int SQLITE_SAVEPOINT = 32;

    /** arg1: null, arg2: null. */
    public static final int SQLITE_RECURSIVE = 33;

    /**
     * Registers an authorizer on the connection, replacing any existing one.
     *
     * @param conn the SQLite connection
     * @param authorizer the authorizer, or {@code null} to clear it
     * @throws SQLException if the connection is not an open SQLite connection
     */
    public static void setAuthorizer(Connection conn, Authorizer authorizer)
            throws SQLException {
        if (!(conn instanceof SQLiteConnection)) {
            throw new SQLException("connection must be to an SQLite db");
        }
        if (conn.isClosed()) {
            throw new SQLException("connection closed");
        }
        ((SQLiteConnection) conn).getDatabase().set_authorizer(authorizer);
    }

    /**
     * Removes the authorizer registered on the connection, if any.
     *
     * @param conn the SQLite connection
     * @throws SQLException if the connection is not an open SQLite connection
     */
    public static void clearAuthorizer(Connection conn) throws SQLException {
        setAuthorizer(conn, null);
    }

    /**
     * Called by SQLite while preparing a statement.
     *
     * @param actionCode one of the action constants of this class, e.g. {@link #SQLITE_READ}
     * @param arg1 first action-specific argument (documented on each action constant), may be null
     * @param arg2 second action-specific argument (documented on each action constant), may be
     *     null
     * @param database the database name ("main", "temp", or attached name), may be null
     * @param triggerOrView name of the innermost trigger or view responsible for the access, or
     *     null if the access comes directly from top-level SQL
     * @return {@link #SQLITE_OK}, {@link #SQLITE_DENY} or {@link #SQLITE_IGNORE}
     */
    protected abstract int authorize(
            int actionCode, String arg1, String arg2, String database, String triggerOrView);

    /** Called from JNI with raw (standard) UTF-8 arguments. */
    private int xAuthorize(int actionCode, byte[] arg1, byte[] arg2, byte[] db, byte[] trg) {
        return authorize(actionCode, utf8(arg1), utf8(arg2), utf8(db), utf8(trg));
    }

    private static String utf8(byte[] bytes) {
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }
}
