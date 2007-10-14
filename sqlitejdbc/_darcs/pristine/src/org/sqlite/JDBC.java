/* Copyright 2006 David Crawshaw, see LICENSE file for licensing [BSD]. */
package org.sqlite;

import java.sql.*;
import java.util.*;

public class JDBC implements Driver
{
    private static final String PREFIX = "jdbc:sqlite:";

    static {
        try {
            DriverManager.registerDriver(new JDBC());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getMajorVersion() { return 1; }
    public int getMinorVersion() { return 1; }

    public boolean jdbcCompliant() { return false; }

    public boolean acceptsURL(String url) {
        return url != null && url.toLowerCase().startsWith(PREFIX);
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
            throws SQLException {
        return new DriverPropertyInfo[] {};
    }

    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) return null;
        url = url.trim();

        // if no file name is given use a memory database
        return new Conn(url, PREFIX.equalsIgnoreCase(url) ?
            ":memory:" : url.substring(PREFIX.length()));
    }
}
