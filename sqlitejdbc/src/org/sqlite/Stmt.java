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

import java.sql.*;
import java.util.ArrayList;

/** See comment in RS.java to explain the strange inheritance hierarchy. */
class Stmt extends RS implements Statement, Codes
{
    private ArrayList batch = null;

    Stmt(Conn conn) { super(conn); }

    /** Calls sqlite3_step() and sets up results. Expects a clean stmt. */
    protected boolean exec() throws SQLException {
        if (pointer == 0) throw new SQLException(
            "SQLite JDBC internal error: pointer == 0 on exec.");
        if (isRS()) throw new SQLException(
            "SQLite JDBC internal error: isRS() on exec.");

        boolean rc = false;
        try {
            rc = db.execute(this, null);
        } finally {
            resultsWaiting = rc;
        }

        return db.column_count(pointer) != 0;
    }


    // PUBLIC INTERFACE /////////////////////////////////////////////

    public Statement getStatement() { return this; }

    /** More lax than JDBC spec, a Statement can be reused after close().
     *  This is to support Stmt and RS sharing a heap object. */
    public void close() throws SQLException {
        if (pointer == 0) return;
        clearRS();
        colsMeta = null;
        meta = null;
        batch = null;
        int resp = db.finalize(this);
        if (resp != SQLITE_OK && resp != SQLITE_MISUSE)
            db.throwex();
    }

    /** The JVM does not ensure finalize() is called, so a Map in the
     *  DB class keeps track of statements for finalization. */
    protected void finalize() throws SQLException { close(); }

    public int getUpdateCount() throws SQLException {
        checkOpen();
        if (pointer == 0 || resultsWaiting) return -1;
        return db.changes();
    }

    public boolean execute(String sql) throws SQLException {
        checkOpen(); close();
        this.sql = sql;
        db.prepare(this);
        return exec();
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        checkOpen(); close();
        this.sql = sql;
        db.prepare(this);
        if (!exec()) {
            close();
            throw new SQLException("query does not return ResultSet");
        }
        return getResultSet();
    }

    public int executeUpdate(String sql) throws SQLException {
        checkOpen(); close();
        this.sql = sql;
        int changes = 0;
        try {
            db.prepare(this);
            changes = db.executeUpdate(this, null);
        } finally { close(); }
        return changes;
    }

    public void addBatch(String sql) throws SQLException {
        checkOpen();
        if (batch == null) batch = new ArrayList();
        batch.add(sql);
    }

    public void clearBatch() throws SQLException {
        checkOpen(); if (batch != null) batch.clear(); }

    public int[] executeBatch() throws SQLException {
        // TODO: optimise
        checkOpen(); close();
        if (batch == null) return new int[] {};

        int[] changes = new int[batch.size()];

        synchronized (db) { try {
            for (int i=0; i < changes.length; i++) {
                try {
                    sql = (String)batch.get(i);
                    db.prepare(this);
                    changes[i] = db.executeUpdate(this, null);
                } catch (SQLException e) {
                    throw new BatchUpdateException(
                        "batch entry " + i + ": " + e.getMessage(), changes);
                } finally {
                    db.finalize(this);
                }
            }
        } finally {
            batch.clear();
        } }

        return changes;
    }
}
