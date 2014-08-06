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

package org.sqlite.core;

import java.sql.SQLException;

import org.sqlite.Function;
import org.sqlite.SQLiteJDBCLoader;

/** This class provides a thin JNI layer over the SQLite3 C API. */
public final class NativeDB extends DB
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
    public static boolean load() throws Exception {
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
     * @see org.sqlite.core.DB#_open(java.lang.String, int)
     */
    @Override
    protected native synchronized void _open(String file, int openFlags) throws SQLException;

    /**
     * @see org.sqlite.core.DB#_close()
     */
    @Override
    protected native synchronized void _close() throws SQLException;

    /**
     * @see org.sqlite.core.DB#_exec(java.lang.String)
     */
    @Override
    public native synchronized int _exec(String sql) throws SQLException;

    /**
     * @see org.sqlite.core.DB#shared_cache(boolean)
     */
    @Override
    public native synchronized int shared_cache(boolean enable);

    /**
     * @see org.sqlite.core.DB#enable_load_extension(boolean)
     */
    @Override
    public native synchronized int enable_load_extension(boolean enable);

    /**
     * @see org.sqlite.core.DB#interrupt()
     */
    @Override
    public native void interrupt();

    /**
     * @see org.sqlite.core.DB#busy_timeout(int)
     */
    @Override
    public native synchronized void busy_timeout(int ms);

    /**
     * @see org.sqlite.core.DB#prepare(java.lang.String)
     */
    //native synchronized void exec(String sql) throws SQLException;
    @Override
    protected native synchronized long prepare(String sql) throws SQLException;

    /**
     * @see org.sqlite.core.DB#errmsg()
     */
    @Override
    native synchronized String errmsg();

    /**
     * @see org.sqlite.core.DB#libversion()
     */
    @Override
    public native synchronized String libversion();

    /**
     * @see org.sqlite.core.DB#changes()
     */
    @Override
    public native synchronized int changes();

    /**
     * @see org.sqlite.core.DB#total_changes()
     */
    @Override
    public native synchronized int total_changes();

    /**
     * @see org.sqlite.core.DB#finalize(long)
     */
    @Override
    protected native synchronized int finalize(long stmt);

    /**
     * @see org.sqlite.core.DB#step(long)
     */
    @Override
    public native synchronized int step(long stmt);

    /**
     * @see org.sqlite.core.DB#reset(long)
     */
    @Override
    public native synchronized int reset(long stmt);

    /**
     * @see org.sqlite.core.DB#clear_bindings(long)
     */
    @Override
    public native synchronized int clear_bindings(long stmt);

    /**
     * @see org.sqlite.core.DB#bind_parameter_count(long)
     */
    @Override
    native synchronized int bind_parameter_count(long stmt);

    /**
     * @see org.sqlite.core.DB#column_count(long)
     */
    @Override
    public native synchronized int column_count(long stmt);

    /**
     * @see org.sqlite.core.DB#column_type(long, int)
     */
    @Override
    public native synchronized int column_type(long stmt, int col);

    /**
     * @see org.sqlite.core.DB#column_decltype(long, int)
     */
    @Override
    public native synchronized String column_decltype(long stmt, int col);

    /**
     * @see org.sqlite.core.DB#column_table_name(long, int)
     */
    @Override
    public native synchronized String column_table_name(long stmt, int col);

    /**
     * @see org.sqlite.core.DB#column_name(long, int)
     */
    @Override
    public native synchronized String column_name(long stmt, int col);

    /**
     * @see org.sqlite.core.DB#column_text(long, int)
     */
    @Override
    public native synchronized String column_text(long stmt, int col);

    /**
     * @see org.sqlite.core.DB#column_blob(long, int)
     */
    @Override
    public native synchronized byte[] column_blob(long stmt, int col);

    /**
     * @see org.sqlite.core.DB#column_double(long, int)
     */
    @Override
    public native synchronized double column_double(long stmt, int col);

    /**
     * @see org.sqlite.core.DB#column_long(long, int)
     */
    @Override
    public native synchronized long column_long(long stmt, int col);

    /**
     * @see org.sqlite.core.DB#column_int(long, int)
     */
    @Override
    public native synchronized int column_int(long stmt, int col);

    /**
     * @see org.sqlite.core.DB#bind_null(long, int)
     */
    @Override
    native synchronized int bind_null(long stmt, int pos);

    /**
     * @see org.sqlite.core.DB#bind_int(long, int, int)
     */
    @Override
    native synchronized int bind_int(long stmt, int pos, int v);

    /**
     * @see org.sqlite.core.DB#bind_long(long, int, long)
     */
    @Override
    native synchronized int bind_long(long stmt, int pos, long v);

    /**
     * @see org.sqlite.core.DB#bind_double(long, int, double)
     */
    @Override
    native synchronized int bind_double(long stmt, int pos, double v);

    /**
     * @see org.sqlite.core.DB#bind_text(long, int, java.lang.String)
     */
    @Override
    native synchronized int bind_text(long stmt, int pos, String v);

    /**
     * @see org.sqlite.core.DB#bind_blob(long, int, byte[])
     */
    @Override
    native synchronized int bind_blob(long stmt, int pos, byte[] v);

    /**
     * @see org.sqlite.core.DB#result_null(long)
     */
    @Override
    public native synchronized void result_null(long context);

    /**
     * @see org.sqlite.core.DB#result_text(long, java.lang.String)
     */
    @Override
    public native synchronized void result_text(long context, String val);

    /**
     * @see org.sqlite.core.DB#result_blob(long, byte[])
     */
    @Override
    public native synchronized void result_blob(long context, byte[] val);

    /**
     * @see org.sqlite.core.DB#result_double(long, double)
     */
    @Override
    public native synchronized void result_double(long context, double val);

    /**
     * @see org.sqlite.core.DB#result_long(long, long)
     */
    @Override
    public native synchronized void result_long(long context, long val);

    /**
     * @see org.sqlite.core.DB#result_int(long, int)
     */
    @Override
    public native synchronized void result_int(long context, int val);

    /**
     * @see org.sqlite.core.DB#result_error(long, java.lang.String)
     */
    @Override
    public native synchronized void result_error(long context, String err);

    /**
     * @see org.sqlite.core.DB#value_bytes(org.sqlite.Function, int)
     */
    @Override
    public native synchronized int value_bytes(Function f, int arg);

    /**
     * @see org.sqlite.core.DB#value_text(org.sqlite.Function, int)
     */
    @Override
    public native synchronized String value_text(Function f, int arg);

    /**
     * @see org.sqlite.core.DB#value_blob(org.sqlite.Function, int)
     */
    @Override
    public native synchronized byte[] value_blob(Function f, int arg);

    /**
     * @see org.sqlite.core.DB#value_double(org.sqlite.Function, int)
     */
    @Override
    public native synchronized double value_double(Function f, int arg);

    /**
     * @see org.sqlite.core.DB#value_long(org.sqlite.Function, int)
     */
    @Override
    public native synchronized long value_long(Function f, int arg);

    /**
     * @see org.sqlite.core.DB#value_int(org.sqlite.Function, int)
     */
    @Override
    public native synchronized int value_int(Function f, int arg);

    /**
     * @see org.sqlite.core.DB#value_type(org.sqlite.Function, int)
     */
    @Override
    public native synchronized int value_type(Function f, int arg);

    /**
     * @see org.sqlite.core.DB#create_function(java.lang.String, org.sqlite.Function)
     */
    @Override
    public native synchronized int create_function(String name, Function func);

    /**
     * @see org.sqlite.core.DB#destroy_function(java.lang.String)
     */
    @Override
    public native synchronized int destroy_function(String name);

    /**
     * @see org.sqlite.core.DB#free_functions()
     */
    @Override
    native synchronized void free_functions();

    /**
     * @see org.sqlite.core.DB#backup(java.lang.String, java.lang.String, org.sqlite.core.DB.ProgressObserver)
     */
    @Override
    public native synchronized int backup(String dbName, String destFileName, ProgressObserver observer) throws SQLException;

    /**
     * @see org.sqlite.core.DB#restore(java.lang.String, java.lang.String,
     *      org.sqlite.core.DB.ProgressObserver)
     */
    @Override
    public native synchronized int restore(String dbName, String sourceFileName, ProgressObserver observer)
            throws SQLException;

    // COMPOUND FUNCTIONS (for optimisation) /////////////////////////

    /**
     * Provides metadata for table columns.
     * @returns For each column returns: <br/>
     * res[col][0] = true if column constrained NOT NULL<br/>
     * res[col][1] = true if column is part of the primary key<br/>
     * res[col][2] = true if column is auto-increment.
     * @see org.sqlite.core.DB#column_metadata(long)
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
