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
 * The two subclasses, NativeDB and NestedDB, provide the actual access to
 * SQLite functions.
 */
abstract class DB implements Codes
{
    /** The JDBC Connection that 'owns' this database instance. */
    Conn                          conn   = null;

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
     * @param ms The time length in milliseconds for setting the timeout value.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/busy_timeout.html">http://www.sqlite.org/c3ref/busy_timeout.html</a>
     */
    abstract void busy_timeout(int ms) throws SQLException;

    /**
     * Return English-language text that describes the error as either UTF-8 or UTF-16.
     * @return The error description in English.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/errcode.html">http://www.sqlite.org/c3ref/errcode.html</a>
     */
    abstract String errmsg() throws SQLException;

    /**
     * Returns the information as the SQLITE_VERSION, SQLITE_VERSION_NUMBER, and SQLITE_SOURCE_ID C
     * preprocessor macros but are associated with the library instead of the header file.
     * @return The compile-time SQLite version information.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/libversion.html">http://www.sqlite.org/c3ref/libversion.html</a>
     * @see <a href="http://www.sqlite.org/c3ref/c_source_id.html">http://www.sqlite.org/c3ref/c_source_id.html</a>
     */
    abstract String libversion() throws SQLException;

    /**
     * Returns the number of database rows that were changed or inserted or deleted by the SQL
     * statement.
     * @return The number of database rows
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/changes.html">http://www.sqlite.org/c3ref/changes.html</a>
     */
    abstract int changes() throws SQLException;

    /**
     * Enables or disables the sharing of the database cache and schema data structures between
     * connections to the same database.
     * @param enable True for enabling; false otherwise.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/enable_shared_cache.html">http://www.sqlite.org/c3ref/enable_shared_cache.html</a>
     * @see org.sqlite.SQLiteErrorCode
     */
    abstract int shared_cache(boolean enable) throws SQLException;

    /**
     * Enables or disables extension loading.
     * @param enable True to enable extension loading; false otherwise.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/load_extension.html">http://www.sqlite.org/c3ref/load_extension.html</a>
     */
    abstract int enable_load_extension(boolean enable) throws SQLException;

    /**
     * Executes an SQL statement using the process of compiling, evaluating, and destroying the
     * prepared statement object..
     * @param sql The SQL statement to be executed.
     * @throws SQLException
     * @see <a href=""></a>
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
     * Opens a given SQLite database file using open flags and the database connection.
     * @param conn The database connection object.
     * @param file The SQLite database file name.
     * @param openFlags Flags for file open operations:<br/>
     *  0x00000001 (SQLITE_OPEN_READONLY),<br/>
     *  0x00000002 (SQLITE_OPEN_READWRITE),<br/>
     *  0x00000004 (SQLITE_OPEN_CREATE),<br/>
     *  0x00000008 (SQLITE_OPEN_DELETEONCLOSE),<br/>
     *  0x00000010 (SQLITE_OPEN_EXCLUSIVE),<br/>
     *  0x00000020 (SQLITE_OPEN_AUTOPROXY),<br/>
     *  0x00000040 (SQLITE_OPEN_URI),<br/>
     *  0x00000080 (SQLITE_OPEN_MEMORY),<br/>
     *  0x00000100 (SQLITE_OPEN_MAIN_DB),<br/>
     *  0x00000200 (SQLITE_OPEN_TEMP_DB),<br/>
     *  0x00000400 (SQLITE_OPEN_TRANSIENT_DB),<br/>
     *  0x00000800 (SQLITE_OPEN_MAIN_JOURNAL),<br/>
     *  0x00001000 (SQLITE_OPEN_TEMP_JOURNAL),<br/>
     *  0x00002000 (SQLITE_OPEN_SUBJOURNAL),<br/>
     *  0x00004000 (SQLITE_OPEN_MASTER_JOURNAL),<br/>
     *  0x00008000 (SQLITE_OPEN_NOMUTEX),<br/>
     *  0x00010000 (SQLITE_OPEN_FULLMUTEX),<br/>
     *  0x00020000 (SQLITE_OPEN_SHAREDCACHE),<br/>
     *  0x00040000 (SQLITE_OPEN_PRIVATECACHE),<br/>
     *  0x00080000 (SQLITE_OPEN_WAL)
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/open.html">http://www.sqlite.org/c3ref/open.html</a> 
     * @see <a href="http://www.sqlite.org/c3ref/c_open_autoproxy.html"> http://www.sqlite.org/c3ref/c_open_autoproxy.html</a>
     */
    final synchronized void open(Conn conn, String file, int openFlags) throws SQLException {
        this.conn = conn;
        _open(file, openFlags);
    }

    /**
     * Closes a database connection and finalizes any remaining statements before the closing
     * operation.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/close.html">http://www.sqlite.org/c3ref/close.html</a>
     */
    final synchronized void close() throws SQLException {
        // finalize any remaining statements before closing db
        synchronized (stmts) {
            Iterator i = stmts.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry) i.next();
                Stmt stmt = (Stmt) entry.getValue();
                finalize(((Long) entry.getKey()).longValue());
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
     * Complies the SQL statement
     * @param stmt The Stmt object to be compiled.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/prepare.html">http://www.sqlite.org/c3ref/prepare.html</a>
     */
    final synchronized void prepare(Stmt stmt) throws SQLException {
        if (stmt.pointer != 0)
            finalize(stmt);
        stmt.pointer = prepare(stmt.sql);
        stmts.put(new Long(stmt.pointer), stmt);
    }

    /**
     * Destroy the prepared statement object
     * @param stmt The Stmt object to be destroyed.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/finalize.html">http://www.sqlite.org/c3ref/finalize.html</a>
     */
    final synchronized int finalize(Stmt stmt) throws SQLException {
        if (stmt.pointer == 0)
            return 0;
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
     * Opens the database file with the given openFlags.
     * @param filename The SQLite database file name.
     * @param openFlags <a href="http://www.sqlite.org/c3ref/c_open_autoproxy.html">Flags for file open operations</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/open.html">http://www.sqlite.org/c3ref/open.html</a>
     */
    protected abstract void _open(String filename, int openFlags) throws SQLException;

    /**
     * Closes the database connection.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/close.html">http://www.sqlite.org/c3ref/close.html</a>
     */
    protected abstract void _close() throws SQLException;

    /**
     * Complies, evaluates, executes and commits the SQL statement.
     * @param sql The SQL statement.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/exec.html">http://www.sqlite.org/c3ref/exec.html</a>
     */
    protected abstract int _exec(String sql) throws SQLException;

    /**
     * Complies the given SQL statement.
     * @param sql The given SQL statement.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/prepare.html">http://www.sqlite.org/c3ref/prepare.html</a> 
     */
    protected abstract long prepare(String sql) throws SQLException;

    /**
     * Destroys a prepared statement object with the pointer to the statement object.
     * @param stmt Pointer to the statement object.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/finalize.html">http://www.sqlite.org/c3ref/finalize.html</a>
     */
    protected abstract int finalize(long stmt) throws SQLException;

    /**
     * Evaluates an SQL statement with the pointer to the statement object.
     * @param stmt Pointer to the statement object.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/step.html">http://www.sqlite.org/c3ref/step.html</a>
     */
    protected abstract int step(long stmt) throws SQLException;

    /**
     * Reset a prepared statement object back to its initial state, ready to be re-executed with the
     * pointer to the statement object.
     * @param stmt Pointer to the statement object.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/reset.html">http://www.sqlite.org/c3ref/reset.html</a>
     */
    protected abstract int reset(long stmt) throws SQLException;

    /**
     * Reset all bindings on a prepared statement (reset all host parameters to NULL) with the
     * pointer to the statement object.
     * @param stmt Pointer to the statement object.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/clear_bindings.html">http://www.sqlite.org/c3ref/clear_bindings.html</a>
     */
    abstract int clear_bindings(long stmt) throws SQLException; // TODO remove?

    /**
     * Gets the number of parameters in a prepared SQL statement with the pointer to the statement
     * object.
     * @param stmt Pointer to the statement object.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/bind_parameter_count.html">http://www.sqlite.org/c3ref/bind_parameter_count.html</a>
     */
    abstract int bind_parameter_count(long stmt) throws SQLException;

    /**
     * Gets the number of columns in the result set returned by the prepared statement with the
     * statement object.
     * @param stmt Pointer to the statement object.
     * @return The number of columns in the result set
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_count.html">http://www.sqlite.org/c3ref/column_count.html</a>
     */
    abstract int column_count(long stmt) throws SQLException;

    /**
     * Gets the datatype code for the initial data type of the result column with the pointer to the
     * the number of the column.
     * @param stmt Pointer to the statement object.
     * @param col The number of column.
     * @return The datatype code
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">http://www.sqlite.org/c3ref/column_blob.html</a>
     */
    abstract int column_type(long stmt, int col) throws SQLException;

    /**
     * Gets the declared type of the table column for prepared statement with the pointer to the
     * statement object and the number of the column.
     * @param stmt Pointer to the statement object.
     * @param col The number of column.
     * @return The declared type
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_decltype.html">http://www.sqlite.org/c3ref/column_decltype.html</a>
     */
    abstract String column_decltype(long stmt, int col) throws SQLException;

    /**
     * Gets the original text which is the declared type of the column in the CREATE TABLE statement
     * with the pointer to the statement object and the number of the column.
     * @param stmt Pointer to the statement object.
     * @param col The number of column.
     * @return The original column name.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_database_name.html">http://www.sqlite.org/c3ref/column_database_name.html</a>
     */
    abstract String column_table_name(long stmt, int col) throws SQLException;

    /**
     * Gets the name assigned to a particular column in the result set of a SELECT statement with
     * the pointer to the statement object and the number of the column.
     * @param stmt Pointer to the statement object.
     * @param col The number of column.
     * @return The column name.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_name.html">http://www.sqlite.org/c3ref/column_name.html</a>
     */
    abstract String column_name(long stmt, int col) throws SQLException;

    /**
     * Gets the text of column in the result set of a SELECT statement with the pointer to the
     * statement object and the number of the column.
     * @param stmt Pointer to the statement object.
     * @param col The number of column.
     * @return The value of the column as text data type.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">http://www.sqlite.org/c3ref/column_blob.html</a>
     */
    abstract String column_text(long stmt, int col) throws SQLException;

    /**
     * Gets the BLOB value of column in the result set of a SELECT statement with the pointer to the
     * statement object and the number of the column.
     * @param stmt Pointer to the statement object.
     * @param col The number of column.
     * @return The value of the column as blob data type.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">http://www.sqlite.org/c3ref/column_blob.html</a>
     */
    abstract byte[] column_blob(long stmt, int col) throws SQLException;

    /**
     * Gets the double value of column in the result set of a SELECT statement with the pointer to
     * the statement object and the number of the column.
     * @param stmt Pointer to the statement object.
     * @param col The number of column.
     * @return The value of the column as double data type.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">http://www.sqlite.org/c3ref/column_blob.html</a>
     */
    abstract double column_double(long stmt, int col) throws SQLException;

    /**
     * Gets the long value of column in the result set of a SELECT statement with the pointer to the
     * statement object and the number of the column.
     * @param stmt Pointer to the statement object.
     * @param col The number of column.
     * @return The value of the column as long data type.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">http://www.sqlite.org/c3ref/column_blob.html</a>
     */
    abstract long column_long(long stmt, int col) throws SQLException;

    /**
     * Gets the int value of column in the result set of a SELECT statement with the pointer to the
     * statement object and the number of the column.
     * @param stmt Pointer to the statement object.
     * @param col The number of column.
     * @return The value of the column as int data type.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">http://www.sqlite.org/c3ref/column_blob.html</a>
     */
    abstract int column_int(long stmt, int col) throws SQLException;

    /**
     * Binds NULL value to prepared statements with the pointer to the statement object and the
     * index of the SQL parameter to be set to NULL.
     * @param stmt Pointer to the statement object.
     * @param pos The index of the SQL parameter to be set to NULL.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     */
    abstract int bind_null(long stmt, int pos) throws SQLException;

    /**
     * Binds int value to prepared statements with the pointer to the statement object, the index of
     * the SQL parameter to be set and the value to bind to the parameter.
     * @param stmt Pointer to the statement object.
     * @param pos The index of the SQL parameter to be set.
     * @param v The value to bind to the parameter.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">http://www.sqlite.org/c3ref/bind_blob.html</a>
     */
    abstract int bind_int(long stmt, int pos, int v) throws SQLException;

    /**
     * Binds long value to prepared statements with the pointer to the statement object, the index
     * of the SQL parameter to be set and the value to bind to the parameter.
     * @param stmt Pointer to the statement object.
     * @param pos The index of the SQL parameter to be set.
     * @param v The value to bind to the parameter.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">http://www.sqlite.org/c3ref/bind_blob.html</a>
     */
    abstract int bind_long(long stmt, int pos, long v) throws SQLException;

    /**
     * Binds double value to prepared statements with the pointer to the statement object, the index
     * of the SQL parameter to be set and the value to bind to the parameter.
     * @param stmt Pointer to the statement object.
     * @param pos The index of the SQL parameter to be set.
     * @param v The value to bind to the parameter.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">http://www.sqlite.org/c3ref/bind_blob.html</a>
     */
    abstract int bind_double(long stmt, int pos, double v) throws SQLException;

    /**
     * Binds text value to prepared statements with the pointer to the statement object, the index
     * of the SQL parameter to be set and the value to bind to the parameter.
     * @param stmt Pointer to the statement object.
     * @param pos The index of the SQL parameter to be set.
     * @param v The value to bind to the parameter.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">http://www.sqlite.org/c3ref/bind_blob.html</a>
     */
    abstract int bind_text(long stmt, int pos, String v) throws SQLException;

    /**
     * Binds blob value to prepared statements with the pointer to the statement object, the index
     * of the SQL parameter to be set and the value to bind to the parameter.
     * @param stmt Pointer to the statement object.
     * @param pos The index of the SQL parameter to be set.
     * @param v The value to bind to the parameter.
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
     * @param val The result value of an SQL function.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/result_blob.html">http://www.sqlite.org/c3ref/result_blob.html</a>
     */
    abstract void result_text(long context, String val) throws SQLException;

    /**
     * Sets the result of an SQL function as blob data type with the pointer to the SQLite database
     * context and the the result value of byte array.
     * @param context Pointer to the SQLite database context.
     * @param val The result value of an SQL function.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/result_blob.html">http://www.sqlite.org/c3ref/result_blob.html</a>
     */
    abstract void result_blob(long context, byte[] val) throws SQLException;

    /**
     * Sets the result of an SQL function as double data type with the pointer to the SQLite
     * database context and the the result value of double.
     * @param context Pointer to the SQLite database context.
     * @param val The result value of an SQL function.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/result_blob.html">http://www.sqlite.org/c3ref/result_blob.html</a>
     */
    abstract void result_double(long context, double val) throws SQLException;

    /**
     * Sets the result of an SQL function as long data type with the pointer to the SQLite database
     * context and the the result value of long.
     * @param context Pointer to the SQLite database context.
     * @param val The result value of an SQL function.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/result_blob.html">http://www.sqlite.org/c3ref/result_blob.html</a>
     */
    abstract void result_long(long context, long val) throws SQLException;

    /**
     * Sets the result of an SQL function as int data type with the pointer to the SQLite database
     * context and the the result value of int.
     * @param context Pointer to the SQLite database context.
     * @param val The result value of an SQL function.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/result_blob.html">http://www.sqlite.org/c3ref/result_blob.html</a>
     */
    abstract void result_int(long context, int val) throws SQLException;

    /**
     * Sets the result of an SQL function as an error with the pointer to the SQLite database
     * context and the the error of String.
     * @param context Pointer to the SQLite database context.
     * @param err The error result of an SQL function.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/result_blob.html">http://www.sqlite.org/c3ref/result_blob.html</a>
     */
    abstract void result_error(long context, String err) throws SQLException;

    /**
     * Accesses the parameter value on the function or aggregate in bytes data type with the
     * function object and the parameter value.
     * @param f The function object.
     * @param arg The parameter value of the function or aggregate.
     * @return The parameter value of the function or aggregate.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/value_blob.html">http://www.sqlite.org/c3ref/value_blob.html</a>
     */
    abstract int value_bytes(Function f, int arg) throws SQLException;

    /**
     * Accesses the parameter values on the function or aggregate in text data type with the
     * function object and the parameter value.
     * @param f The function object.
     * @param arg The parameter value of the function or aggregate.
     * @return The parameter value of the function or aggregate.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/value_blob.html">http://www.sqlite.org/c3ref/value_blob.html</a>
     */
    abstract String value_text(Function f, int arg) throws SQLException;

    /**
     * Accesses the parameter values on the function or aggregate in blob data type with the
     * function object and the parameter value.
     * @param f The function object.
     * @param arg The parameter value of the function or aggregate.
     * @return The parameter value of the function or aggregate.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/value_blob.html">http://www.sqlite.org/c3ref/value_blob.html</a>
     */
    abstract byte[] value_blob(Function f, int arg) throws SQLException;

    /**
     * Accesses the parameter values on the function or aggregate in double data type with the
     * function object and the parameter value.
     * @param f The function object.
     * @param arg The parameter value of the function or aggregate.
     * @return The parameter value of the function or aggregate.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/value_blob.html">http://www.sqlite.org/c3ref/value_blob.html</a>
     */
    abstract double value_double(Function f, int arg) throws SQLException;

    /**
     * Accesses the parameter values on the function or aggregate in long data type with the
     * function object and the parameter value.
     * @param f The function object.
     * @param arg The parameter value of the function or aggregate.
     * @return The parameter value of the function or aggregate.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/value_blob.html">http://www.sqlite.org/c3ref/value_blob.html</a>
     */
    abstract long value_long(Function f, int arg) throws SQLException;

    /**
     * Accesses the parameter values on the function or aggregate in int data type with the function
     * object and the parameter value.
     *@param f The function object.
     * @param arg The parameter value of the function or aggregate.
     * @return The parameter value of the function or aggregate.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/value_blob.html">http://www.sqlite.org/c3ref/value_blob.html</a>
     */
    abstract int value_int(Function f, int arg) throws SQLException;

    /**
     * Accesses the parameter values on the function or aggregate in NONE data type with the
     * function object and the parameter value.
     * @param f The function object.
     * @param arg The parameter value of the function or aggregate.
     * @return The parameter value of the function or aggregate.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/value_blob.html">http://www.sqlite.org/c3ref/value_blob.html</a>
     */
    abstract int value_type(Function f, int arg) throws SQLException;

    /**
     * Create a user defined function with given function name and the function object.
     * @param name The function name to be created.
     * @param f The function object.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/create_function.html">http://www.sqlite.org/c3ref/create_function.html</a>
     */
    abstract int create_function(String name, Function f) throws SQLException;

    /**
     * De-registers a user defined function
     * @param name The name of the function to de-registered.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see NestedDB.c
     */
    abstract int destroy_function(String name) throws SQLException;

    /**
     * Unused as we use the user_data pointer to store a single word.
     * @throws SQLException
     */
    abstract void free_functions() throws SQLException;

    /**
     * Backups a given database to target file with using a ProgressObserver object.
     * @param dbName The database name to be backed up.
     * @param destFileName The target buckup file name.
     * @param observer The ProgressObserver object.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href=""></a>
     */
    abstract int backup(String dbName, String destFileName, ProgressObserver observer) throws SQLException;

    /**
     * Restores database,
     * @param dbName The database name.
     * @param sourceFileName The source file name for restoring the database.
     * @param observer The ProgressObserver object.
     * @return <a href="http://www.sqlite.org/c3ref/c_abort.html">Result Codes</a>
     * @throws SQLException
     * @see <a href=""></a>
     */
    abstract int restore(String dbName, String sourceFileName, ProgressObserver observer) throws SQLException;

    public static interface ProgressObserver
    {
        public void progress(int remaining, int pageCount);
    }

    /**
     * Returns an array describing the attributes (not null, primary key and auto increment) of a
     * column.
     * @param stmt Pointer to the statement object.
     * @return The column attribute array.<br/>
     * index[col][0] = true if column constrained NOT NULL;<br/>
     * index[col][1] = true if column is part of the primary key; <br/>
     * index[col][2] = true if column is auto-increment.
     * @throws SQLException
     */
    abstract boolean[][] column_metadata(long stmt) throws SQLException;

    // COMPOUND FUNCTIONS ////////////////////////////////////////////

    /**
     * Gets the column names in the result set of a SELECT statement.
     * @param stmt The Stmt object.
     * @return String array of column names.
     * @throws SQLException
     */
    final synchronized String[] column_names(long stmt) throws SQLException {
        String[] names = new String[column_count(stmt)];
        for (int i = 0; i < names.length; i++)
            names[i] = column_name(stmt, i);
        return names;
    }

    /**
     * Bind values to prepared statements
     * @param stmt Pointer to the statement object.
     * @param pos The index of the SQL parameter to be set to NULL.
     * @param v The value to bind to the parameter.
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
     * @param stmt The pointer of Stmt object.
     * @param count The number of SQL statements.
     * @param vals The array of parameter values.
     * @return Array of the number of rows changed or inserted or deleted for each command if all
     *         commands execute successfully;
     * @throws SQLException
     */
    final synchronized int[] executeBatch(long stmt, int count, Object[] vals) throws SQLException {
        if (count < 1)
            throw new SQLException("count (" + count + ") < 1");

        final int params = bind_parameter_count(stmt);

        int rc;
        int[] changes = new int[count];

        try {
            for (int i = 0; i < count; i++) {
                reset(stmt);
                for (int j = 0; j < params; j++)
                    if (sqlbind(stmt, j, vals[(i * params) + j]) != SQLITE_OK)
                        throwex();

                rc = step(stmt);
                if (rc != SQLITE_DONE) {
                    reset(stmt);
                    if (rc == SQLITE_ROW)
                        throw new BatchUpdateException("batch entry " + i + ": query returns results", changes);
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
     * @param stmt The Stmt object.
     * @param vals The array of parameter values.
     * @return True if a row of ResultSet is ready; false otherwise.
     * @throws SQLException
     */
    final synchronized boolean execute(Stmt stmt, Object[] vals) throws SQLException {
        if (vals != null) {
            final int params = bind_parameter_count(stmt.pointer);
            if (params != vals.length)
                throw new SQLException("assertion failure: param count (" + params + ") != value count (" + vals.length
                        + ")");

            for (int i = 0; i < params; i++)
                if (sqlbind(stmt.pointer, i, vals[i]) != SQLITE_OK)
                    throwex();
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
     * @return The number of database rows that were changed or inserted or deleted by the most
     *         recently completed SQL.
     * @throws SQLException
     */
    final synchronized int executeUpdate(Stmt stmt, Object[] vals) throws SQLException {
        if (execute(stmt, vals))
            throw new SQLException("query returns results");
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
     * @param errorCode The error code to be passed.
     * @throws SQLException
     */
    final void throwex(int errorCode) throws SQLException {
        throw newSQLException(errorCode);
    }

    /**
     * Throws SQL Exception with error code and message.
     * @param errorCode The error code to be passed.
     * @param errorMessage The error message to be passed.
     * @throws SQLException
     */
    final void throwex(int errorCode, String errorMessage) throws SQLException {
        throw newSQLException(errorCode, errorMessage);
    }

    /**
     * Throws formated SQLException with error code and message.
     * @param errorCode The error code to be passed.
     * @param errorMessage The error message to be passed.
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
     * @param errorCode The error code to be passed.
     * @return SQLException with error code and message.
     * @throws SQLException
     */
    private SQLException newSQLException(int errorCode) throws SQLException {
        return newSQLException(errorCode, errmsg());
    }

    /*
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
        if (!conn.getAutoCommit())
            return;

        if (begin == 0)
            begin = prepare("begin;");
        if (commit == 0)
            commit = prepare("commit;");

        try {
            if (step(begin) != SQLITE_DONE)
                return; // assume we are in a transaction
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
