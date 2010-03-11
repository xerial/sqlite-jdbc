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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;

public class JDBC implements Driver
{
    public static final String PREFIX = "jdbc:sqlite:";

    static {
        try {
            DriverManager.registerDriver(new JDBC());
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getMajorVersion() {
        return SQLiteJDBCLoader.getMajorVersion();
    }

    public int getMinorVersion() {
        return SQLiteJDBCLoader.getMinorVersion();
    }

    public boolean jdbcCompliant() {
        return false;
    }

    public boolean acceptsURL(String url) {
        return isValidURL(url);
    }

    public static boolean isValidURL(String url) {
        return url != null && url.toLowerCase().startsWith(PREFIX);
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return SQLiteConfig.getDriverPropertyInfo();
    }

    public Connection connect(String url, Properties info) throws SQLException {
        return createConnection(url, info);
    }

    static String extractAddress(String url) {
        // if no file name is given use a memory database
        return PREFIX.equalsIgnoreCase(url) ? ":memory:" : url.substring(PREFIX.length());
    }

    public static Connection createConnection(String url, Properties prop) throws SQLException {
        if (!isValidURL(url))
            throw new SQLException("invalid database address: " + url);

        url = url.trim();
        return new Conn(url, extractAddress(url), prop);
    }
}
