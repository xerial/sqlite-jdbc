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
import java.util.Properties;
import java.util.concurrent.Executor;

import org.sqlite.jdbc4.JDBC4Connection;

public class SQLiteConnection extends JDBC4Connection
{
    /**
     * Constructor to create a connection to a database at the given location.
     * @param url The location of the database.
     * @param fileName The database.
     * @throws SQLException
     */
    public SQLiteConnection(String url, String fileName) throws SQLException {
        this(url, fileName, new Properties());
    }

    /**
     * Constructor to create a pre-configured connection to a database at the
     * given location.
     * @param url The location of the database file.
     * @param fileName The database.
     * @param prop The configurations to apply.
     * @throws SQLException
     */
    public SQLiteConnection(String url, String fileName, Properties prop) throws SQLException {
        super(url, fileName, prop);
    }
    public void setSchema(String schema) throws SQLException {
        // TODO
    }
    public String getSchema() throws SQLException {
        // TODO
        return null;
    }
    public void abort(Executor executor) throws SQLException {
        // TODO
    }
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        // TODO
    }
    public int getNetworkTimeout() throws SQLException {
        // TODO
        return 0;
    }
}
