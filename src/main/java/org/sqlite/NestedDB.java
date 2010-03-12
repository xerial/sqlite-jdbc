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

import java.io.PrintWriter;
import java.sql.SQLException;

import org.ibex.nestedvm.Runtime;

// FEATURE: strdup is wasteful, SQLite interface will take unterminated char*

/** Communicates with the Java version of SQLite provided by NestedVM. */
final class NestedDB extends DB implements Runtime.CallJavaCB
{
    /** database pointer */
    int                handle    = 0;

    /** sqlite binary embedded in nestedvm */
    private Runtime    rt        = null;

    /** user defined functions referenced by position (stored in used data) */
    private Function[] functions = null;
    private String[]   funcNames = null;

    // WRAPPER FUNCTIONS ////////////////////////////////////////////

    @Override
    protected synchronized void _open(String filename, int openFlags) throws SQLException {
        if (handle != 0)
            throw new SQLException("DB already open");

        // handle silly windows drive letter mapping
        if (filename.length() > 2) {
            char drive = Character.toLowerCase(filename.charAt(0));
            if (filename.charAt(1) == ':' && drive >= 'a' && drive <= 'z') {

                // convert to nestedvm's "/c:/file" format
                filename = filename.substring(2);
                filename = filename.replace('\\', '/');
                filename = "/" + drive + ":" + filename;
            }
        }

        // start the nestedvm runtime
        try {
            rt = (Runtime) Class.forName("org.sqlite.SQLite").newInstance();
            rt.start();
        }
        catch (Exception e) {
            throw new CausedSQLException(e);
        }

        // callback for user defined functions
        rt.setCallJavaCB(this);

        // open the db and retrieve sqlite3_db* pointer
        int passback = rt.xmalloc(4);
        int str = rt.strdup(filename);
        //      if (call("sqlite3_open_v2", str, openFlags, 0, passback) != SQLITE_OK)
        if (call("sqlite3_open_v2", str, passback, openFlags, 0) != SQLITE_OK)
            throwex();
        handle = deref(passback);
        rt.free(str);
        rt.free(passback);
    }

    /* callback for Runtime.CallJavaCB above */
    public int call(int xType, int context, int args, int value) {
        xUDF(xType, context, args, value);
        return 0;
    }

    @Override
    protected synchronized void _close() throws SQLException {
        if (handle == 0)
            return;
        try {
            if (call("sqlite3_close", handle) != SQLITE_OK)
                throwex();
        }
        finally {
            handle = 0;
            rt.stop();
            rt = null;
        }
    }

    @Override
    int shared_cache(boolean enable) throws SQLException {
        // The shared cache is per-process, so it is useless as
        // each nested connection is its own process.
        return -1;
    }

    @Override
    int enable_load_extension(boolean enable) throws SQLException {
        // TODO enable_load_extension is not supported  in pure-java mode
        //return call("sqlite3_enable_load_extension", handle, enable ? 1 : 0);
        return 1;
    }

    @Override
    synchronized void interrupt() throws SQLException {
        call("sqlite3_interrupt", handle);
    }

    @Override
    synchronized void busy_timeout(int ms) throws SQLException {
        call("sqlite3_busy_timeout", handle, ms);
    }

    @Override
    protected synchronized long prepare(String sql) throws SQLException {

        int passback = rt.xmalloc(4);
        int str = rt.strdup(sql);
        int ret = call("sqlite3_prepare_v2", handle, str, -1, passback, 0);
        rt.free(str);
        if (ret != SQLITE_OK) {
            rt.free(passback);
            throwex(ret);
        }
        int pointer = deref(passback);
        rt.free(passback);
        return pointer;
    }

    @Override
    synchronized String errmsg() throws SQLException {
        return cstring(call("sqlite3_errmsg", handle));
    }

    @Override
    synchronized String libversion() throws SQLException {
        return cstring(call("sqlite3_libversion", handle));
    }

    @Override
    synchronized int changes() throws SQLException {
        return call("sqlite3_changes", handle);
    }

    @Override
    protected synchronized int _exec(String sql) throws SQLException {
        if (rt == null)
            throw DB.newSQLException(SQLiteErrorCode.SQLITE_MISUSE.code, "attempt to use the closed conection");
        int passback = rt.xmalloc(4);
        int str = rt.strdup(sql);
        int status = call("sqlite3_exec", handle, str, 0, 0, passback);
        if (status != SQLITE_OK) {
            String errorMessage = cstring(passback);
            call("sqlite3_free", deref(passback));
            rt.free(passback);
            throwex(status, errorMessage);
        }
        rt.free(passback);
        return status;
    }

    @Override
    protected synchronized int finalize(long stmt) throws SQLException {
        return call("sqlite3_finalize", (int) stmt);
    }

    @Override
    protected synchronized int step(long stmt) throws SQLException {
        return call("sqlite3_step", (int) stmt);
    }

    @Override
    protected synchronized int reset(long stmt) throws SQLException {
        return call("sqlite3_reset", (int) stmt);
    }

    @Override
    synchronized int clear_bindings(long stmt) throws SQLException {
        return call("sqlite3_clear_bindings", (int) stmt);
    }

    @Override
    synchronized int bind_parameter_count(long stmt) throws SQLException {
        return call("sqlite3_bind_parameter_count", (int) stmt);
    }

    @Override
    synchronized int column_count(long stmt) throws SQLException {
        return call("sqlite3_column_count", (int) stmt);
    }

    @Override
    synchronized int column_type(long stmt, int col) throws SQLException {
        return call("sqlite3_column_type", (int) stmt, col);
    }

    @Override
    synchronized String column_name(long stmt, int col) throws SQLException {
        return utfstring(call("sqlite3_column_name", (int) stmt, col));
    }

    @Override
    synchronized String column_text(long stmt, int col) throws SQLException {
        return utfstring(call("sqlite3_column_text", (int) stmt, col));
    }

    @Override
    synchronized byte[] column_blob(long stmt, int col) throws SQLException {
        int addr = call("sqlite3_column_blob", (int) stmt, col);
        if (addr == 0)
            return null;
        byte[] blob = new byte[call("sqlite3_column_bytes", (int) stmt, col)];
        copyin(addr, blob, blob.length);
        return blob;
    }

    @Override
    synchronized double column_double(long stmt, int col) throws SQLException {
        try {
            return Double.parseDouble(column_text(stmt, col));
        }
        catch (NumberFormatException e) {
            return Double.NaN;
        } // TODO
    }

    @Override
    synchronized long column_long(long stmt, int col) throws SQLException {
        try {
            return Long.parseLong(column_text(stmt, col));
        }
        catch (NumberFormatException e) {
            return 0;
        } // TODO
    }

    @Override
    synchronized int column_int(long stmt, int col) throws SQLException {
        return call("sqlite3_column_int", (int) stmt, col);
    }

    @Override
    synchronized String column_decltype(long stmt, int col) throws SQLException {
        return utfstring(call("sqlite3_column_decltype", (int) stmt, col));
    }

    @Override
    synchronized String column_table_name(long stmt, int col) throws SQLException {
        return utfstring(call("sqlite3_column_table_name", (int) stmt, col));
    }

    @Override
    synchronized int bind_null(long stmt, int pos) throws SQLException {
        return call("sqlite3_bind_null", (int) stmt, pos);
    }

    @Override
    synchronized int bind_int(long stmt, int pos, int v) throws SQLException {
        return call("sqlite3_bind_int", (int) stmt, pos, v);
    }

    @Override
    synchronized int bind_long(long stmt, int pos, long v) throws SQLException {
        return bind_text(stmt, pos, Long.toString(v)); // TODO
    }

    @Override
    synchronized int bind_double(long stmt, int pos, double v) throws SQLException {
        return bind_text(stmt, pos, Double.toString(v)); // TODO
    }

    @Override
    synchronized int bind_text(long stmt, int pos, String v) throws SQLException {
        if (v == null)
            return bind_null(stmt, pos);
        return call("sqlite3_bind_text", (int) stmt, pos, rt.strdup(v), -1, rt.lookupSymbol("free"));
    }

    @Override
    synchronized int bind_blob(long stmt, int pos, byte[] buf) throws SQLException {
        if (buf == null || buf.length < 1)
            return bind_null(stmt, pos);
        int len = buf.length;
        int blob = rt.xmalloc(len); // free()ed by sqlite3_bind_blob
        copyout(buf, blob, len);
        return call("sqlite3_bind_blob", (int) stmt, pos, blob, len, rt.lookupSymbol("free"));
    }

    @Override
    synchronized void result_null(long cxt) throws SQLException {
        call("sqlite3_result_null", (int) cxt);
    }

    @Override
    synchronized void result_text(long cxt, String val) throws SQLException {
        call("sqlite3_result_text", (int) cxt, rt.strdup(val), -1, rt.lookupSymbol("free"));
    }

    @Override
    synchronized void result_blob(long cxt, byte[] val) throws SQLException {
        if (val == null || val.length == 0) {
            result_null(cxt);
            return;
        }
        int blob = rt.xmalloc(val.length);
        copyout(val, blob, val.length);
        call("sqlite3_result_blob", (int) cxt, blob, val.length, rt.lookupSymbol("free"));
    }

    @Override
    synchronized void result_double(long cxt, double val) throws SQLException {
        result_text(cxt, Double.toString(val));
    } // TODO

    @Override
    synchronized void result_long(long cxt, long val) throws SQLException {
        result_text(cxt, Long.toString(val));
    } // TODO

    @Override
    synchronized void result_int(long cxt, int val) throws SQLException {
        call("sqlite3_result_int", (int) cxt, val);
    }

    @Override
    synchronized void result_error(long cxt, String err) throws SQLException {
        int str = rt.strdup(err);
        call("sqlite3_result_error", (int) cxt, str, -1);
        rt.free(str);
    }

    @Override
    synchronized int value_bytes(Function f, int arg) throws SQLException {
        return call("sqlite3_value_bytes", value(f, arg));
    }

    @Override
    synchronized String value_text(Function f, int arg) throws SQLException {
        return utfstring(call("sqlite3_value_text", value(f, arg)));
    }

    @Override
    synchronized byte[] value_blob(Function f, int arg) throws SQLException {
        int addr = call("sqlite3_value_blob", value(f, arg));
        if (addr == 0)
            return null;
        byte[] blob = new byte[value_bytes(f, arg)];
        copyin(addr, blob, blob.length);
        return blob;
    }

    @Override
    synchronized double value_double(Function f, int arg) throws SQLException {
        return Double.parseDouble(value_text(f, arg)); // TODO
    }

    @Override
    synchronized long value_long(Function f, int arg) throws SQLException {
        return Long.parseLong(value_text(f, arg)); // TODO
    }

    @Override
    synchronized int value_int(Function f, int arg) throws SQLException {
        return call("sqlite3_value_int", value(f, arg));
    }

    @Override
    synchronized int value_type(Function f, int arg) throws SQLException {
        return call("sqlite3_value_type", value(f, arg));
    }

    private int value(Function f, int arg) throws SQLException {
        return deref((int) f.value + (arg * 4));
    }

    @Override
    synchronized int create_function(String name, Function func) throws SQLException {
        if (functions == null) {
            functions = new Function[10];
            funcNames = new String[10];
        }

        // find a position
        int pos;
        for (pos = 0; pos < functions.length; pos++)
            if (functions[pos] == null)
                break;

        if (pos == functions.length) { // expand function arrays
            Function[] fnew = new Function[functions.length * 2];
            String[] nnew = new String[funcNames.length * 2];
            System.arraycopy(functions, 0, fnew, 0, functions.length);
            System.arraycopy(funcNames, 0, nnew, 0, funcNames.length);
            functions = fnew;
            funcNames = nnew;
        }

        // register function
        functions[pos] = func;
        funcNames[pos] = name;
        int rc;
        int str = rt.strdup(name);
        rc = call("create_function_helper", handle, str, pos, func instanceof Function.Aggregate ? 1 : 0);
        rt.free(str);
        return rc;
    }

    @Override
    synchronized int destroy_function(String name) throws SQLException {
        if (name == null)
            return 0;

        // find function position number
        int pos;
        for (pos = 0; pos < funcNames.length; pos++)
            if (name.equals(funcNames[pos]))
                break;
        if (pos == funcNames.length)
            return 0;

        functions[pos] = null;
        funcNames[pos] = null;

        // deregister function
        int rc;
        int str = rt.strdup(name);
        rc = call("create_function_helper", handle, str, -1, 0);
        rt.free(str);
        return rc;
    }

    /* unused as we use the user_data pointer to store a single word */
    @Override
    synchronized void free_functions() {}

    /** Callback used by xFunc (1), xStep (2) and xFinal (3). */
    synchronized void xUDF(int xType, int context, int args, int value) {
        Function func = null;

        try {
            int pos = call("sqlite3_user_data", context);
            func = functions[pos];
            if (func == null)
                throw new SQLException("function state inconsistent");

            func.context = context;
            func.value = value;
            func.args = args;

            switch (xType) {
            case 1:
                func.xFunc();
                break;
            case 2:
                ((Function.Aggregate) func).xStep();
                break;
            case 3:
                ((Function.Aggregate) func).xFinal();
                break;
            }
        }
        catch (SQLException e) {
            try {
                String err = e.toString();
                if (err == null)
                    err = "unknown error";
                int str = rt.strdup(err);
                call("sqlite3_result_error", context, str, -1);
                rt.free(str);
            }
            catch (SQLException exp) {
                exp.printStackTrace();//TODO
            }
        }
        finally {
            if (func != null) {
                func.context = 0;
                func.value = 0;
                func.args = 0;
            }
        }
    }

    /** Calls support function found in upstream/sqlite-metadata.patch */
    @Override
    synchronized boolean[][] column_metadata(long stmt) throws SQLException {
        int colCount = call("sqlite3_column_count", (int) stmt);
        boolean[][] meta = new boolean[colCount][3];
        int pass;

        pass = rt.xmalloc(12); // struct metadata

        for (int i = 0; i < colCount; i++) {
            call("column_metadata_helper", handle, (int) stmt, i, pass);
            meta[i][0] = deref(pass) == 1;
            meta[i][1] = deref(pass + 4) == 1;
            meta[i][2] = deref(pass + 8) == 1;
        }

        rt.free(pass);
        return meta;
    }

    @Override
    int backup(String dbName, String destFileName, ProgressObserver observer) throws SQLException {
        throw new SQLException("backup command is not supported in pure-java mode");
    }

    @Override
    int restore(String dbName, String sourceFileName, ProgressObserver observer) throws SQLException {
        throw new SQLException("restore command is not supported in pure-java mode");
    }

    // HELPER FUNCTIONS /////////////////////////////////////////////

    /** safe to reuse parameter arrays as all functions are syncrhonized */
    private final int[] p0 = new int[] {}, p1 = new int[] { 0 }, p2 = new int[] { 0, 0 }, p3 = new int[] { 0, 0, 0 },
            p4 = new int[] { 0, 0, 0, 0 }, p5 = new int[] { 0, 0, 0, 0, 0 };

    private int call(String addr, int a0) throws SQLException {
        p1[0] = a0;
        return call(addr, p1);
    }

    private int call(String addr, int a0, int a1) throws SQLException {
        p2[0] = a0;
        p2[1] = a1;
        return call(addr, p2);
    }

    private int call(String addr, int a0, int a1, int a2) throws SQLException {
        p3[0] = a0;
        p3[1] = a1;
        p3[2] = a2;
        return call(addr, p3);
    }

    private int call(String addr, int a0, int a1, int a2, int a3) throws SQLException {
        p4[0] = a0;
        p4[1] = a1;
        p4[2] = a2;
        p4[3] = a3;
        return call(addr, p4);
    }

    private int call(String addr, int a0, int a1, int a2, int a3, int a4) throws SQLException {
        p5[0] = a0;
        p5[1] = a1;
        p5[2] = a2;
        p5[3] = a3;
        p5[4] = a4;
        return call(addr, p5);
    }

    private int call(String func, int[] args) throws SQLException {
        try {
            return rt.call(func, args);
        }
        catch (Runtime.CallException e) {
            throw new CausedSQLException(e);
        }
    }

    /** Dereferences a pointer, returning the word it points to. */
    private int deref(int pointer) throws SQLException {
        try {
            return rt.memRead(pointer);
        }
        catch (Runtime.ReadFaultException e) {
            throw new CausedSQLException(e);
        }
    }

    private String utfstring(int str) throws SQLException {
        try {
            return rt.utfstring(str);
        }
        catch (Runtime.ReadFaultException e) {
            throw new CausedSQLException(e);
        }
    }

    private String cstring(int str) throws SQLException {
        try {
            return rt.cstring(str);
        }
        catch (Runtime.ReadFaultException e) {
            throw new CausedSQLException(e);
        }
    }

    private void copyin(int addr, byte[] buf, int count) throws SQLException {
        try {
            rt.copyin(addr, buf, count);
        }
        catch (Runtime.ReadFaultException e) {
            throw new CausedSQLException(e);
        }
    }

    private void copyout(byte[] buf, int addr, int count) throws SQLException {
        try {
            rt.copyout(buf, addr, count);
        }
        catch (Runtime.FaultException e) {
            throw new CausedSQLException(e);
        }
    }

    /** Maps any exception onto an SQLException. */
    private static final class CausedSQLException extends SQLException
    {
        private final Exception cause;

        CausedSQLException(Exception e) {
            if (e == null)
                throw new RuntimeException("null exception cause");
            cause = e;
        }

        @Override
        public Throwable getCause() {
            return cause;
        }

        @Override
        public void printStackTrace() {
            cause.printStackTrace();
        }

        @Override
        public void printStackTrace(PrintWriter s) {
            cause.printStackTrace(s);
        }

        @Override
        public Throwable fillInStackTrace() {
            return cause.fillInStackTrace();
        }

        @Override
        public StackTraceElement[] getStackTrace() {
            return cause.getStackTrace();
        }

        @Override
        public String getMessage() {
            return cause.getMessage();
        }
    }
}
