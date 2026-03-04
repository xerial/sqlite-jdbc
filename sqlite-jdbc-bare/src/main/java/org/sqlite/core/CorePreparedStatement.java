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

import java.sql.Date;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteConnectionConfig;
import org.sqlite.date.FastDateFormat;
import org.sqlite.jdbc3.JDBC3Connection;
import org.sqlite.jdbc4.JDBC4Statement;

public abstract class CorePreparedStatement extends JDBC4Statement {
    protected int columnCount;
    protected int paramCount;
    protected int batchQueryCount;

    /**
     * Constructs a prepared statement on a provided connection.
     *
     * @param conn Connection on which to create the prepared statement.
     * @param sql The SQL script to prepare.
     * @throws SQLException
     */
    protected CorePreparedStatement(SQLiteConnection conn, String sql) throws SQLException {
        super(conn);

        this.sql = sql;
        DB db = conn.getDatabase();
        db.prepare(this);
        rs.colsMeta = pointer.safeRun(DB::column_names);
        columnCount = pointer.safeRunInt(DB::column_count);
        paramCount = pointer.safeRunInt(DB::bind_parameter_count);
        batchQueryCount = 0;
        batch = null;
        batchPos = 0;
    }

    /** @see org.sqlite.jdbc3.JDBC3Statement#executeBatch() */
    @Override
    public int[] executeBatch() throws SQLException {
        return Arrays.stream(executeLargeBatch()).mapToInt(l -> (int) l).toArray();
    }

    /** @see org.sqlite.jdbc3.JDBC3Statement#executeLargeBatch() */
    @Override
    public long[] executeLargeBatch() throws SQLException {
        if (batchQueryCount == 0) {
            return new long[] {};
        }

        if (this.conn instanceof JDBC3Connection) {
            ((JDBC3Connection) this.conn).tryEnforceTransactionMode();
        }

        return this.withConnectionTimeout(
                () -> {
                    try {
                        return conn.getDatabase()
                                .executeBatch(
                                        pointer, batchQueryCount, batch, conn.getAutoCommit());
                    } finally {
                        clearBatch();
                    }
                });
    }

    /** @see org.sqlite.jdbc3.JDBC3Statement#clearBatch() () */
    @Override
    public void clearBatch() throws SQLException {
        super.clearBatch();
        batchQueryCount = 0;
    }

    // PARAMETER FUNCTIONS //////////////////////////////////////////

    /**
     * Assigns the object value to the element at the specific position of array batch.
     *
     * @param pos
     * @param value
     * @throws SQLException
     */
    protected void batch(int pos, Object value) throws SQLException {
        checkOpen();
        if (batch == null) {
            batch = new Object[paramCount];
        }
        batch[batchPos + pos - 1] = value;
    }

    /** Store the date in the user's preferred format (text, int, or real) */
    protected void setDateByMilliseconds(int pos, Long value, Calendar calendar)
            throws SQLException {
        SQLiteConnectionConfig config = conn.getConnectionConfig();
        switch (config.getDateClass()) {
            case TEXT:
                batch(
                        pos,
                        FastDateFormat.getInstance(
                                        config.getDateStringFormat(), calendar.getTimeZone())
                                .format(new Date(value)));
                break;

            case REAL:
                // long to Julian date
                batch(pos, new Double((value / 86400000.0) + 2440587.5));
                break;

            default: // INTEGER:
                batch(pos, new Long(value / config.getDateMultiplier()));
        }
    }
}
