/*
 * Copyright (c) 2007 David Crawshaw <david@zentus.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.sqlite;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
 * This class is the interface to SQLite. It provides some helper functions
 * used by other parts of the driver. The goal of the helper functions here
 * are not only to provide functionality, but to handle contractual
 * differences between the JDBC specification and the SQLite C API.
 *
 * The process of moving SQLite weirdness into this class is incomplete.
 * You'll still find lots of code in Stmt and PrepStmt that are doing
 * implicit contract conversions. Sorry.
 *
 * The subclass, NativeDB, provides the actual access to SQLite functions.
 */
abstract class DB implements Codes
{
    /** The JDBC Connection that 'owns' this database instance. */
    SQLiteConnection                          conn   = null;

    /** The "begin;"and "commit;" statement handles. */
    long                          begin  = 0;
    long                          commit = 0;

    /** Tracer for statements to avoid unfinalized statements on db close. */
    private final Map<Long, Stmt> stmts  = new HashMap<Long, Stmt>();

    // WRAPPER FUNCTIONS ////////////////////////////////////////////

    /**
     * Aborts any pending operation and returns at its earliest opportunity.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/interrupt.html">http://www.sqlite.org/c3ref/interrupt.html</a>
     */
    abstract void interrupt() throws SQLException;

    /**
     * Sets a <a href="http://www.sqlite.org/c3ref/busy_handler.html">busy handler</a> that sleeps
     * for a specified amount of time when a table is locked.
     * @param ms Time to sleep in milliseconds.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/busy_timeout.html">http://www.sqlite.org/c3ref/busy_timeout.html</a>
     */
    abstract void busy_timeout(int ms) throws SQLException;

    /**
     * Return English-language text that describes the error as either UTF-8 or UTF-16.
     * @return Error description in English.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/errcode.html">http://www.sqlite.org/c3ref/errcode.html</a>
     */
    abstract String errmsg() throws SQLException;

    /**
     * Returns the value for SQLITE_VERSION, SQLITE_VERSION_NUMBER, and SQLITE_SOURCE_ID C
     * preprocessor macros that are associated with the library.
     * @return Compile-time SQLite version information.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/libversion.html">http://www.sqlite.org/c3ref/libversion.html</a>
     * @see <a href="http://www.sqlite.org/c3ref/c_source_id.html">http://www.sqlite.org/c3ref/c_source_id.html</a>
     */
    abstract String libversion() throws SQLException;

    /**
     * @return Number of rows that were changed, inserted or deleted by the last
     * SQL statement
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/changes.html">http://www.sqlite.org/c3ref/changes.html</a>
     */
    abstract int changes() throws SQLException;

    /**
     * @return Number of row changes caused by INSERT, UPDATE or DELETE statements
     * since the database connection was opened.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/total_changes.html">http://www.sqlite.org/c3ref/total_changes.html</a>
     */
    abstract int total_changes() throws SQLException;

    /**
     * Enables or disables the sharing of the database cache and schema data structures between
     * connections to the same database.
     * @param enable True to enable; false otherwise.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/enable_shared_cache.html">http://www.sqlite.org/c3ref/enable_shared_cache.html</a>
     * @see org.sqlite.SQLiteErrorCode
     */
    abstract int shared_cache(boolean enable) throws SQLException;

    /**
     * Enables or disables loading of SQLite extensions.
     * @param enable True to enable; false otherwise.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/load_extension.html">http://www.sqlite.org/c3ref/load_extension.html</a>
     */
    abstract int enable_load_extension(boolean enable) throws SQLException;

    /**
     * Executes an SQL statement using the process of compiling, evaluating, and destroying the
     * prepared statement object.
     * @param sql SQL statement to be executed.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/exec.html">http://www.sqlite.org/c3ref/exec.html</a>
     */
    final synchronized void exec(String sql) throws SQLException {
        long pointer = 0;
        try {
            pointer = prepare(sql);
            switch (step(pointer)) {
            case SQLITE_DONE:
                ensureAutoCommit();
                return;
            case SQLITE_ROW:
                return;
            default:
                throwex();
            }
        }
        finally {
            finalize(pointer);
        }
    }

    /**
     * Creates an SQLite interface to a database for the given connection.
     * @param conn The connection.
     * @param file The database.
     * @param openFlags File opening configurations
     * (<a href="http://www.sqlite.org/c3ref/c_open_autoproxy.html">http://www.sqlite.org/c3ref/c_open_autoproxy.html</a>)
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/open.html">http://www.sqlite.org/c3ref/open.html</a>
     */
    final synchronized void open(SQLiteConnection conn, String file, int openFlags) throws SQLException {
        this.conn = conn;
        _open(file, openFlags);
    }

    /**
     * Closes a database connection and finalizes any remaining statements before
     * the closing operation.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/close.html">http://www.sqlite.org/c3ref/close.html</a>
     */
    final synchronized void close() throws SQLException {
        // finalize any remaining statements before closing db
        synchronized (stmts) {
            Iterator<Map.Entry<Long, Stmt>> i = stmts.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<Long, Stmt> entry = i.next();
                Stmt stmt = entry.getValue();
                finalize(entry.getKey().longValue());
                if (stmt != null) {
                    stmt.pointer = 0;
                }
                i.remove();
            }
        }

        // remove memory used by user-defined functions
        free_functions();

        // clean up commit object
        if (begin != 0) {
            finalize(begin);
            begin = 0;
        }
        if (commit != 0) {
            finalize(commit);
            commit = 0;
        }

        _close();
    }

    /**
     * Complies the an SQL statement.
     * @param stmt The SQL statement to compile.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/prepare.html">http://www.sqlite.org/c3ref/prepare.html</a>
     */
    final synchronized void prepare(Stmt stmt) throws SQLException {
        if (stmt.pointer != 0) {
            finalize(stmt);
        }
        stmt.pointer = prepare(stmt.sql);
        stmts.put(new Long(stmt.pointer), stmt);
    }

    /**
     * Destroys a statement.
     * @param stmt The statement to destroy.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/finalize.html">http://www.sqlite.org/c3ref/finalize.html</a>
     */
    final synchronized int finalize(Stmt stmt) throws SQLException {
        if (stmt.pointer == 0) {
            return 0;
        }
        int rc = SQLITE_ERROR;
        try {
            rc = finalize(stmt.pointer);
        }
        finally {
            stmts.remove(new Long(stmt.pointer));
            stmt.pointer = 0;
        }
        return rc;
    }

    /**
     * Creates an SQLite interface to a database with the provided open flags.
     * @param filename The database to open.
     * @param openFlags File opening configurations
     * (<a href="http://www.sqlite.org/c3ref/c_open_autoproxy.html">http://www.sqlite.org/c3ref/c_open_autoproxy.html</a>)
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/open.html">http://www.sqlite.org/c3ref/open.html</a>
     */
    protected abstract void _open(String filename, int openFlags) throws SQLException;

    /**
     * Closes the SQLite interface to a database.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/close.html">http://www.sqlite.org/c3ref/close.html</a>
     */
    protected abstract void _close() throws SQLException;

    /**
     * Complies, evaluates, executes and commits an SQL statement.
     * @param sql An SQL statement.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/exec.html">http://www.sqlite.org/c3ref/exec.html</a>
     */
    protected abstract int _exec(String sql) throws SQLException;

    /**
     * Complies an SQL statement.
     * @param sql An SQL statement.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/prepare.html">http://www.sqlite.org/c3ref/prepare.html</a>
     */
    protected abstract long prepare(String sql) throws SQLException;

    /**
     * Destroys a prepared statement.
     * @param stmt Pointer to the statement pointer.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/finalize.html">http://www.sqlite.org/c3ref/finalize.html</a>
     */
    protected abstract int finalize(long stmt) throws SQLException;

    /**
     * Evaluates a statement.
     * @param stmt Pointer to the statement.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/step.html">http://www.sqlite.org/c3ref/step.html</a>
     */
    protected abstract int step(long stmt) throws SQLException;

    /**
     * Sets a prepared statement object back to its initial state,
     * ready to be re-executed.
     * @param stmt Pointer to the statement.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/reset.html">http://www.sqlite.org/c3ref/reset.html</a>
     */
    protected abstract int reset(long stmt) throws SQLException;

    /**
     * Reset all bindings on a prepared statement (reset all host parameters to NULL).
     * @param stmt Pointer to the statement.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/clear_bindings.html">http://www.sqlite.org/c3ref/clear_bindings.html</a>
     */
    abstract int clear_bindings(long stmt) throws SQLException; // TODO remove?

    /**
     * @param stmt Pointer to the statement.
     * @return Number of parameters in a prepared SQL.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/bind_parameter_count.html">http://www.sqlite.org/c3ref/bind_parameter_count.html</a>
     */
    abstract int bind_parameter_count(long stmt) throws SQLException;

    /**
     * @param stmt Pointer to the statement.
     * @return Number of columns in the result set returned by the prepared statement.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_count.html">http://www.sqlite.org/c3ref/column_count.html</a>
     */
    abstract int column_count(long stmt) throws SQLException;

    /**
     * @param stmt Pointer to the statement.
     * @param col Number of column.
     * @return Datatype code for the initial data type of the result column.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">http://www.sqlite.org/c3ref/column_blob.html</a>
     */
    abstract int column_type(long stmt, int col) throws SQLException;

    /**
     * @param stmt Pointer to the statement.
     * @param col Number of column.
     * @return Declared type of the table column for prepared statement.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_decltype.html">http://www.sqlite.org/c3ref/column_decltype.html</a>
     */
    abstract String column_decltype(long stmt, int col) throws SQLException;

    /**
     * @param stmt Pointer to the statement.
     * @param col Number of column.
     * @return Original text of column name which is the declared in the CREATE TABLE statement.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_database_name.html">http://www.sqlite.org/c3ref/column_database_name.html</a>
     */
    abstract String column_table_name(long stmt, int col) throws SQLException;

    /**
     * @param stmt Pointer to the statement.
     * @param col The number of column.
     * @return Name assigned to a particular column in the result set of a SELECT statement.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_name.html">http://www.sqlite.org/c3ref/column_name.html</a>
     */
    abstract String column_name(long stmt, int col) throws SQLException;

    /**
     * @param stmt Pointer to the statement.
     * @param col Number of column.
     * @return Value of the column as text data type in the result set of a SELECT statement.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">http://www.sqlite.org/c3ref/column_blob.html</a>
     */
    abstract String column_text(long stmt, int col) throws SQLException;

    /**
     * @param stmt Pointer to the statement.
     * @param col Number of column.
     * @return BLOB value of the column in the result set of a SELECT statement
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">http://www.sqlite.org/c3ref/column_blob.html</a>
     */
    abstract byte[] column_blob(long stmt, int col) throws SQLException;

    /**
     * @param stmt Pointer to the statement.
     * @param col Number of column.
     * @return DOUBLE value of the column in the result set of a SELECT statement
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">http://www.sqlite.org/c3ref/column_blob.html</a>
     */
    abstract double column_double(long stmt, int col) throws SQLException;

    /**
     * @param stmt Pointer to the statement.
     * @param col Number of column.
     * @return LONG value of the column in the result set of a SELECT statement.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">http://www.sqlite.org/c3ref/column_blob.html</a>
     */
    abstract long column_long(long stmt, int col) throws SQLException;

    /**
     * @param stmt Pointer to the statement.
     * @param col Number of column.
     * @return INT value of column in the result set of a SELECT statement.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">http://www.sqlite.org/c3ref/column_blob.html</a>
     */
    abstract int column_int(long stmt, int col) throws SQLException;

    /**
     * Binds NULL value to prepared statements with the pointer to the statement object and the
     * index of the SQL parameter to be set to NULL.
     * @param stmt Pointer to the statement.
     * @param pos The index of the SQL parameter to be set to NULL.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     */
    abstract int bind_null(long stmt, int pos) throws SQLException;

    /**
     * Binds int value to prepared statements with the pointer to the statement object, the index of
     * the SQL parameter to be set and the value to bind to the parameter.
     * @param stmt Pointer to the statement.
     * @param pos The index of the SQL parameter to be set.
     * @param v Value to bind to the parameter.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">http://www.sqlite.org/c3ref/bind_blob.html</a>
     */
    abstract int bind_int(long stmt, int pos, int v) throws SQLException;

    /**
     * Binds long value to prepared statements with the pointer to the statement object, the index
     * of the SQL parameter to be set and the value to bind to the parameter.
     * @param stmt Pointer to the statement.
     * @param pos The index of the SQL parameter to be set.
     * @param v Value to bind to the parameter.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">http://www.sqlite.org/c3ref/bind_blob.html</a>
     */
    abstract int bind_long(long stmt, int pos, long v) throws SQLException;

    /**
     * Binds double value to prepared statements with the pointer to the statement object, the index
     * of the SQL parameter to be set and the value to bind to the parameter.
     * @param stmt Pointer to the statement.
     * @param pos Index of the SQL parameter to be set.
     * @param v Value to bind to the parameter.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">http://www.sqlite.org/c3ref/bind_blob.html</a>
     */
    abstract int bind_double(long stmt, int pos, double v) throws SQLException;

    /**
     * Binds text value to prepared statements with the pointer to the statement object, the index
     * of the SQL parameter to be set and the value to bind to the parameter.
     * @param stmt Pointer to the statement.
     * @param pos Index of the SQL parameter to be set.
     * @param v value to bind to the parameter.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">http://www.sqlite.org/c3ref/bind_blob.html</a>
     */
    abstract int bind_text(long stmt, int pos, String v) throws SQLException;

    /**
     * Binds blob value to prepared statements with the pointer to the statement object, the index
     * of the SQL parameter to be set and the value to bind to the parameter.
     * @param stmt Pointer to the statement.
     * @param pos Index of the SQL parameter to be set.
     * @param v Value to bind to the parameter.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">http://www.sqlite.org/c3ref/bind_blob.html</a>
     */
    abstract int bind_blob(long stmt, int pos, byte[] v) throws SQLException;

    /**
     * Sets the result of an SQL function as NULL with the pointer to the SQLite database context.
     * @param context Pointer to the SQLite database context.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/result_blob.html">http://www.sqlite.org/c3ref/result_blob.html</a>
     */
    abstract void result_null(long context) throws SQLException;

    /**
     * Sets the result of an SQL function as text data type with the pointer to the SQLite database
     * context and the the result value of String.
     * @param context Pointer to the SQLite database context.
     * @param val Result value of an SQL function.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/result_blob.html">http://www.sqlite.org/c3ref/result_blob.html</a>
     */
    abstract void result_text(long context, String val) throws SQLException;

    /**
     * Sets the result of an SQL function as blob data type with the pointer to the SQLite database
     * context and the the result value of byte array.
     * @param context Pointer to the SQLite database context.
     * @param val Result value of an SQL function.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/result_blob.html">http://www.sqlite.org/c3ref/result_blob.html</a>
     */
    abstract void result_blob(long context, byte[] val) throws SQLException;

    /**
     * Sets the result of an SQL function as double data type with the pointer to the SQLite
     * database context and the the result value of double.
     * @param context Pointer to the SQLite database context.
     * @param val Result value of an SQL function.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/result_blob.html">http://www.sqlite.org/c3ref/result_blob.html</a>
     */
    abstract void result_double(long context, double val) throws SQLException;

    /**
     * Sets the result of an SQL function as long data type with the pointer to the SQLite database
     * context and the the result value of long.
     * @param context Pointer to the SQLite database context.
     * @param val Result value of an SQL function.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/result_blob.html">http://www.sqlite.org/c3ref/result_blob.html</a>
     */
    abstract void result_long(long context, long val) throws SQLException;

    /**
     * Sets the result of an SQL function as int data type with the pointer to the SQLite database
     * context and the the result value of int.
     * @param context Pointer to the SQLite database context.
     * @param val Result value of an SQL function.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/result_blob.html">http://www.sqlite.org/c3ref/result_blob.html</a>
     */
    abstract void result_int(long context, int val) throws SQLException;

    /**
     * Sets the result of an SQL function as an error with the pointer to the SQLite database
     * context and the the error of String.
     * @param context Pointer to the SQLite database context.
     * @param err Error result of an SQL function.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/result_blob.html">http://www.sqlite.org/c3ref/result_blob.html</a>
     */
    abstract void result_error(long context, String err) throws SQLException;

    /**
     * @param f SQLite function object.
     * @param arg Pointer to the parameter of the SQLite function or aggregate.
     * @return Parameter value of the given SQLite function or aggregate in bytes data type.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/value_blob.html">http://www.sqlite.org/c3ref/value_blob.html</a>
     */
    abstract int value_bytes(Function f, int arg) throws SQLException;

    /**
     * @param f SQLite function object.
     * @param arg Pointer to the parameter of the SQLite function or aggregate.
     * @return Parameter value of the given SQLite function or aggregate in text data type.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/value_blob.html">http://www.sqlite.org/c3ref/value_blob.html</a>
     */
    abstract String value_text(Function f, int arg) throws SQLException;

    /**
     * @param f SQLite function object.
     * @param arg Pointer to the parameter of the SQLite function or aggregate.
     * @return Parameter value of the given SQLite function or aggregate in blob data type.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/value_blob.html">http://www.sqlite.org/c3ref/value_blob.html</a>
     */
    abstract byte[] value_blob(Function f, int arg) throws SQLException;

    /**
     * @param f SQLite function object.
     * @param arg Pointer to the parameter of the SQLite function or aggregate.
     * @return Parameter value of the given SQLite function or aggregate in double data type
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/value_blob.html">http://www.sqlite.org/c3ref/value_blob.html</a>
     */
    abstract double value_double(Function f, int arg) throws SQLException;

    /**
     * @param f SQLite function object.
     * @param arg Pointer to the parameter of the SQLite function or aggregate.
     * @return Parameter value of the given SQLite function or aggregate in long data type.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/value_blob.html">http://www.sqlite.org/c3ref/value_blob.html</a>
     */
    abstract long value_long(Function f, int arg) throws SQLException;

    /**
     * Accesses the parameter values on the function or aggregate in int data type with the function
     * object and the parameter value.
     * @param f SQLite function object.
     * @param arg Pointer to the parameter of the SQLite function or aggregate.
     * @return Parameter value of the given SQLite function or aggregate.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/value_blob.html">http://www.sqlite.org/c3ref/value_blob.html</a>
     */
    abstract int value_int(Function f, int arg) throws SQLException;

    /**
     * @param f SQLite function object.
     * @param arg Pointer to the parameter of the SQLite function or aggregate.
     * @return Parameter datatype of the function or aggregate in int data type.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/value_blob.html">http://www.sqlite.org/c3ref/value_blob.html</a>
     */
    abstract int value_type(Function f, int arg) throws SQLException;

    /**
     * Create a user defined function with given function name and the function object.
     * @param name The function name to be created.
     * @param f SQLite function object.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/create_function.html">http://www.sqlite.org/c3ref/create_function.html</a>
     */
    abstract int create_function(String name, Function f) throws SQLException;

    /**
     * De-registers a user defined function
     * @param name Name of the function to de-registered.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     */
    abstract int destroy_function(String name) throws SQLException;

    /**
     * Unused as we use the user_data pointer to store a single word.
     * @throws SQLException
     */
    abstract void free_functions() throws SQLException;

    /**
     * @param dbName Database name to be backed up.
     * @param destFileName Target backup file name.
     * @param observer ProgressObserver object.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     *
     */
    abstract int backup(String dbName, String destFileName, ProgressObserver observer) throws SQLException;

    /**
     * @param dbName Database name for restoring data.
     * @param sourceFileName Source file name.
     * @param observer ProgressObserver object.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     *
     */
    abstract int restore(String dbName, String sourceFileName, ProgressObserver observer) throws SQLException;

    public static interface ProgressObserver
    {
        public void progress(int remaining, int pageCount);
    }

    /**
     * Returns an array describing the attributes (not null, primary key and auto increment) of columns.
     * @param stmt Pointer to the statement.
     * @return Column attribute array.<br/>
     * index[col][0] = true if column constrained NOT NULL;<br/>
     * index[col][1] = true if column is part of the primary key; <br/>
     * index[col][2] = true if column is auto-increment.
     * @throws SQLException
     */
    abstract boolean[][] column_metadata(long stmt) throws SQLException;

    // COMPOUND FUNCTIONS ////////////////////////////////////////////

    /**
     * Returns an array of column names in the result set of the SELECT statement.
     * @param stmt Stmt object.
     * @return String array of column names.
     * @throws SQLException
     */
    final synchronized String[] column_names(long stmt) throws SQLException {
        String[] names = new String[column_count(stmt)];
        for (int i = 0; i < names.length; i++) {
            names[i] = column_name(stmt, i);
        }
        return names;
    }

    /**
     * Bind values to prepared statements
     * @param stmt Pointer to the statement.
     * @param pos Index of the SQL parameter to be set to NULL.
     * @param v Value to bind to the parameter.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">http://www.sqlite.org/c3ref/bind_blob.html</a>
     */
    final synchronized int sqlbind(long stmt, int pos, Object v) throws SQLException {
        pos++;
        if (v == null) {
            return bind_null(stmt, pos);
        }
        else if (v instanceof Integer) {
            return bind_int(stmt, pos, ((Integer) v).intValue());
        }
        else if (v instanceof Short) {
            return bind_int(stmt, pos, ((Short) v).intValue());
        }
        else if (v instanceof Long) {
            return bind_long(stmt, pos, ((Long) v).longValue());
        }
        else if (v instanceof Float) {
            return bind_double(stmt, pos, ((Float) v).doubleValue());
        }
        else if (v instanceof Double) {
            return bind_double(stmt, pos, ((Double) v).doubleValue());
        }
        else if (v instanceof String) {
            return bind_text(stmt, pos, (String) v);
        }
        else if (v instanceof byte[]) {
            return bind_blob(stmt, pos, (byte[]) v);
        }
        else {
            throw new SQLException("unexpected param type: " + v.getClass());
        }
    }

    /**
     * Submits a batch of commands to the database for execution.
     * @see java.sql.Statement#executeBatch()
     * @param stmt Pointer of Stmt object.
     * @param count Number of SQL statements.
     * @param vals Array of parameter values.
     * @return Array of the number of rows changed or inserted or deleted for each command if all
     *         commands execute successfully;
     * @throws SQLException
     */
    final synchronized int[] executeBatch(long stmt, int count, Object[] vals) throws SQLException {
        if (count < 1) {
            throw new SQLException("count (" + count + ") < 1");
        }

        final int params = bind_parameter_count(stmt);

        int rc;
        int[] changes = new int[count];

        try {
            for (int i = 0; i < count; i++) {
                reset(stmt);
                for (int j = 0; j < params; j++) {
                    if (sqlbind(stmt, j, vals[(i * params) + j]) != SQLITE_OK) {
                        throwex();
                    }
                }

                rc = step(stmt);
                if (rc != SQLITE_DONE) {
                    reset(stmt);
                    if (rc == SQLITE_ROW) {
                        throw new BatchUpdateException("batch entry " + i + ": query returns results", changes);
                    }
                    throwex();
                }

                changes[i] = changes();
            }
        }
        finally {
            ensureAutoCommit();
        }

        reset(stmt);
        return changes;
    }

    /**
     * @see <a href="http://www.sqlite.org/c_interface.html#sqlite_exec">http://www.sqlite.org/c_interface.html#sqlite_exec</a>
     * @param stmt Stmt object.
     * @param vals Array of parameter values.
     * @return True if a row of ResultSet is ready; false otherwise.
     * @throws SQLException
     */
    final synchronized boolean execute(Stmt stmt, Object[] vals) throws SQLException {
        if (vals != null) {
            final int params = bind_parameter_count(stmt.pointer);
            if (params != vals.length) {
                throw new SQLException("assertion failure: param count (" + params + ") != value count (" + vals.length
                        + ")");
            }

            for (int i = 0; i < params; i++) {
                if (sqlbind(stmt.pointer, i, vals[i]) != SQLITE_OK) {
                    throwex();
                }
            }
        }

        int statusCode = step(stmt.pointer);
        switch (statusCode) {
        case SQLITE_DONE:
            reset(stmt.pointer);
            ensureAutoCommit();
            return false;
        case SQLITE_ROW:
            return true;
        case SQLITE_BUSY:
        case SQLITE_LOCKED:
        case SQLITE_MISUSE:
            throw newSQLException(statusCode);
        default:
            finalize(stmt);
            throw newSQLException(statusCode);
        }

    }

    /**
     * Executes the given SQL statement using the one-step query execution interface.
     * @param sql SQL statement to be executed.
     * @return True if a row of ResultSet is ready; false otherwise.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/exec.html">http://www.sqlite.org/c3ref/exec.html</a>
     */
    final synchronized boolean execute(String sql) throws SQLException {
        int statusCode = _exec(sql);
        switch (statusCode) {
        case SQLITE_OK:
            return false;
        case SQLITE_DONE:
            ensureAutoCommit();
            return false;
        case SQLITE_ROW:
            return true;
        default:
            throw newSQLException(statusCode);
        }
    }

    /**
     * Execute an SQL INSERT, UPDATE or DELETE statement with the Stmt object and an array of
     * parameter values of the SQL statement..
     * @param stmt Stmt object.
     * @param vals Array of parameter values.
     * @return Number of database rows that were changed or inserted or deleted by the most
     *         recently completed SQL.
     * @throws SQLException
     */
    final synchronized int executeUpdate(Stmt stmt, Object[] vals) throws SQLException {
        if (execute(stmt, vals)) {
            throw new SQLException("query returns results");
        }
        reset(stmt.pointer);
        return changes();
    }

    /**
     * Throws SQLException with error message.
     * @throws SQLException
     */
    final void throwex() throws SQLException {
        throw new SQLException(errmsg());
    }

    /**
     * Throws SQLException with error code.
     * @param errorCode Error code to be passed.
     * @throws SQLException
     */
    final void throwex(int errorCode) throws SQLException {
        throw newSQLException(errorCode);
    }

    /**
     * Throws SQL Exception with error code and message.
     * @param errorCode Error code to be passed.
     * @param errorMessage Error message to be passed.
     * @throws SQLException
     */
    final void throwex(int errorCode, String errorMessage) throws SQLException {
        throw newSQLException(errorCode, errorMessage);
    }

    /**
     * Throws formated SQLException with error code and message.
     * @param errorCode Error code to be passed.
     * @param errorMessage Error message to be passed.
     * @return Formated SQLException with error code and message.
     * @throws SQLException
     */
    static SQLException newSQLException(int errorCode, String errorMessage) throws SQLException {
        SQLiteErrorCode code = SQLiteErrorCode.getErrorCode(errorCode);
        SQLException e = new SQLException(String.format("%s (%s)", code, errorMessage), null, code.code);
        return e;
    }

    /**
     * Throws SQL Exception with error code.
     * @param errorCode Error code to be passed.
     * @return SQLException with error code and message.
     * @throws SQLException
     */
    private SQLException newSQLException(int errorCode) throws SQLException {
        return newSQLException(errorCode, errmsg());
    }

    /**
     * SQLite and the JDBC API have very different ideas about the meaning
     * of auto-commit. Under JDBC, when executeUpdate() returns in
     * auto-commit mode (the default), the programmer assumes the data has
     * been written to disk. In SQLite however, a call to sqlite3_step()
     * with an INSERT statement can return SQLITE_OK, and yet the data is
     * still in limbo.
     *
     * This limbo appears when another statement on the database is active,
     * e.g. a SELECT. SQLite auto-commit waits until the final read
     * statement finishes, and then writes whatever updates have already
     * been OKed. So if a program crashes before the reads are complete,
     * data is lost. E.g:
     *
     *     select begins
     *     insert
     *     select continues
     *     select finishes
     *
     * Works as expected, however
     *
     *     select beings
     *     insert
     *     select continues
     *     crash
     *
     * Results in the data never being written to disk.
     *
     * As a solution, we call "commit" after every statement in auto-commit
     * mode.
     * @throws SQLException
     */
    final void ensureAutoCommit() throws SQLException {
        if (!conn.getAutoCommit()) {
            return;
        }

        if (begin == 0) {
            begin = prepare("begin;");
        }
        if (commit == 0) {
            commit = prepare("commit;");
        }

        try {
            if (step(begin) != SQLITE_DONE)
             {
                return; // assume we are in a transaction
            }
            if (step(commit) != SQLITE_DONE) {
                reset(commit);
                throwex();
            }
            //throw new SQLException("unable to auto-commit");
        }
        finally {
            reset(begin);
            reset(commit);
        }
    }
}
