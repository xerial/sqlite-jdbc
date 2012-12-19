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

import java.sql.SQLException;

/** This class provides a thin JNI layer over the SQLite3 C API. */
final class NativeDB extends DB
{
    /** SQLite connection handle. */
    long                   pointer       = 0;

    private static boolean isLoaded;
    private static boolean loadSucceeded;

    static {
        if ("The Android Project".equals(System.getProperty("java.vm.vendor"))) {
            System.loadLibrary("sqlitejdbc");
            isLoaded = true;
            loadSucceeded = true;
        } else {
            // continue with non Android execution path
            isLoaded = false;
            loadSucceeded = false;
        }
    }

    /**
     * Loads the SQLite interface backend.
     * @return True if the SQLite JDBC driver is successfully loaded; false otherwise.
     */
    static boolean load() throws Exception {
        if (isLoaded)
            return loadSucceeded == true;

        loadSucceeded = SQLiteJDBCLoader.initialize();
        isLoaded = true;
        return loadSucceeded;
    }

    /** linked list of all instanced UDFDatas */
    private final long udfdatalist = 0;

    // WRAPPER FUNCTIONS ////////////////////////////////////////////

    /**
     * @see org.sqlite.DB#_open(java.lang.String, int)
     */
    @Override
    protected native synchronized void _open(String file, int openFlags) throws SQLException;

    /**
     * @see org.sqlite.DB#_close()
     */
    @Override
    protected native synchronized void _close() throws SQLException;

    /**
     * @see org.sqlite.DB#_exec(java.lang.String)
     */
    @Override
    protected native synchronized int _exec(String sql) throws SQLException;

    /**
     * @see org.sqlite.DB#shared_cache(boolean)
     */
    @Override
    native synchronized int shared_cache(boolean enable);

    /**
     * @see org.sqlite.DB#enable_load_extension(boolean)
     */
    @Override
    native synchronized int enable_load_extension(boolean enable);

    /**
     * @see org.sqlite.DB#interrupt()
     */
    @Override
    native void interrupt();

    /**
     * @see org.sqlite.DB#busy_timeout(int)
     */
    @Override
    native synchronized void busy_timeout(int ms);

    /**
     * @see org.sqlite.DB#prepare(java.lang.String)
     */
    //native synchronized void exec(String sql) throws SQLException;
    @Override
    protected native synchronized long prepare(String sql) throws SQLException;

    /**
     * @see org.sqlite.DB#errmsg()
     */
    @Override
    native synchronized String errmsg();

    /**
     * @see org.sqlite.DB#libversion()
     */
    @Override
    native synchronized String libversion();

    /**
     * @see org.sqlite.DB#changes()
     */
    @Override
    native synchronized int changes();

    /**
     * @see org.sqlite.DB#total_changes()
     */
    @Override
    native synchronized int total_changes();

    /**
     * @see org.sqlite.DB#finalize(long)
     */
    @Override
    protected native synchronized int finalize(long stmt);

    /**
     * @see org.sqlite.DB#step(long)
     */
    @Override
    protected native synchronized int step(long stmt);

    /**
     * @see org.sqlite.DB#reset(long)
     */
    @Override
    protected native synchronized int reset(long stmt);

    /**
     * @see org.sqlite.DB#clear_bindings(long)
     */
    @Override
    native synchronized int clear_bindings(long stmt);

    /**
     * @see org.sqlite.DB#bind_parameter_count(long)
     */
    @Override
    native synchronized int bind_parameter_count(long stmt);

    /**
     * @see org.sqlite.DB#column_count(long)
     */
    @Override
    native synchronized int column_count(long stmt);

    /**
     * @see org.sqlite.DB#column_type(long, int)
     */
    @Override
    native synchronized int column_type(long stmt, int col);

    /**
     * @see org.sqlite.DB#column_decltype(long, int)
     */
    @Override
    native synchronized String column_decltype(long stmt, int col);

    /**
     * @see org.sqlite.DB#column_table_name(long, int)
     */
    @Override
    native synchronized String column_table_name(long stmt, int col);

    /**
     * @see org.sqlite.DB#column_name(long, int)
     */
    @Override
    native synchronized String column_name(long stmt, int col);

    /**
     * @see org.sqlite.DB#column_text(long, int)
     */
    @Override
    native synchronized String column_text(long stmt, int col);

    /**
     * @see org.sqlite.DB#column_blob(long, int)
     */
    @Override
    native synchronized byte[] column_blob(long stmt, int col);

    /**
     * @see org.sqlite.DB#column_double(long, int)
     */
    @Override
    native synchronized double column_double(long stmt, int col);

    /**
     * @see org.sqlite.DB#column_long(long, int)
     */
    @Override
    native synchronized long column_long(long stmt, int col);

    /**
     * @see org.sqlite.DB#column_int(long, int)
     */
    @Override
    native synchronized int column_int(long stmt, int col);

    /**
     * @see org.sqlite.DB#bind_null(long, int)
     */
    @Override
    native synchronized int bind_null(long stmt, int pos);

    /**
     * @see org.sqlite.DB#bind_int(long, int, int)
     */
    @Override
    native synchronized int bind_int(long stmt, int pos, int v);

    /**
     * @see org.sqlite.DB#bind_long(long, int, long)
     */
    @Override
    native synchronized int bind_long(long stmt, int pos, long v);

    /**
     * @see org.sqlite.DB#bind_double(long, int, double)
     */
    @Override
    native synchronized int bind_double(long stmt, int pos, double v);

    /**
     * @see org.sqlite.DB#bind_text(long, int, java.lang.String)
     */
    @Override
    native synchronized int bind_text(long stmt, int pos, String v);

    /**
     * @see org.sqlite.DB#bind_blob(long, int, byte[])
     */
    @Override
    native synchronized int bind_blob(long stmt, int pos, byte[] v);

    /**
     * @see org.sqlite.DB#result_null(long)
     */
    @Override
    native synchronized void result_null(long context);

    /**
     * @see org.sqlite.DB#result_text(long, java.lang.String)
     */
    @Override
    native synchronized void result_text(long context, String val);

    /**
     * @see org.sqlite.DB#result_blob(long, byte[])
     */
    @Override
    native synchronized void result_blob(long context, byte[] val);

    /**
     * @see org.sqlite.DB#result_double(long, double)
     */
    @Override
    native synchronized void result_double(long context, double val);

    /**
     * @see org.sqlite.DB#result_long(long, long)
     */
    @Override
    native synchronized void result_long(long context, long val);

    /**
     * @see org.sqlite.DB#result_int(long, int)
     */
    @Override
    native synchronized void result_int(long context, int val);

    /**
     * @see org.sqlite.DB#result_error(long, java.lang.String)
     */
    @Override
    native synchronized void result_error(long context, String err);

    /**
     * @see org.sqlite.DB#value_bytes(org.sqlite.Function, int)
     */
    @Override
    native synchronized int value_bytes(Function f, int arg);

    /**
     * @see org.sqlite.DB#value_text(org.sqlite.Function, int)
     */
    @Override
    native synchronized String value_text(Function f, int arg);

    /**
     * @see org.sqlite.DB#value_blob(org.sqlite.Function, int)
     */
    @Override
    native synchronized byte[] value_blob(Function f, int arg);

    /**
     * @see org.sqlite.DB#value_double(org.sqlite.Function, int)
     */
    @Override
    native synchronized double value_double(Function f, int arg);

    /**
     * @see org.sqlite.DB#value_long(org.sqlite.Function, int)
     */
    @Override
    native synchronized long value_long(Function f, int arg);

    /**
     * @see org.sqlite.DB#value_int(org.sqlite.Function, int)
     */
    @Override
    native synchronized int value_int(Function f, int arg);

    /**
     * @see org.sqlite.DB#value_type(org.sqlite.Function, int)
     */
    @Override
    native synchronized int value_type(Function f, int arg);

    /**
     * @see org.sqlite.DB#create_function(java.lang.String, org.sqlite.Function)
     */
    @Override
    native synchronized int create_function(String name, Function func);

    /**
     * @see org.sqlite.DB#destroy_function(java.lang.String)
     */
    @Override
    native synchronized int destroy_function(String name);

    /**
     * @see org.sqlite.DB#free_functions()
     */
    @Override
    native synchronized void free_functions();

    /**
     * @see org.sqlite.DB#backup(java.lang.String, java.lang.String, org.sqlite.DB.ProgressObserver)
     */
    @Override
    native synchronized int backup(String dbName, String destFileName, ProgressObserver observer) throws SQLException;

    /**
     * @see org.sqlite.DB#restore(java.lang.String, java.lang.String,
     *      org.sqlite.DB.ProgressObserver)
     */
    @Override
    native synchronized int restore(String dbName, String sourceFileName, ProgressObserver observer)
            throws SQLException;

    // COMPOUND FUNCTIONS (for optimisation) /////////////////////////

    /**
     * Provides metadata for table columns.
     * @returns For each column returns: <br/>
     * res[col][0] = true if column constrained NOT NULL<br/>
     * res[col][1] = true if column is part of the primary key<br/>
     * res[col][2] = true if column is auto-increment.
     * @see org.sqlite.DB#column_metadata(long)
     */
    @Override
    native synchronized boolean[][] column_metadata(long stmt);

    /**
     * Throws an SQLException
     * @param msg Message for the SQLException.
     * @throws SQLException
     */
    static void throwex(String msg) throws SQLException {
        throw new SQLException(msg);
    }
}
